---
name: postmottak-feilsoking
description: Undersøk hvorfor en journalpost eller behandling i aap-postmottak-backend står fast, har feilet, er på vent eller har havnet feil sted, i dev eller prod. Dekker drift- og motor-API-ene, loggfelt, en feilsøkingsguide og verifiserte lesende SQL-spørringer for behandlinger, steghistorikk, avklaringsbehov, jobber, fordeling og vurderinger. Bruk ved supportspørsmål, hendelser og spørsmål av typen «hvor er dokument X».
---

# Feilsøking i postmottak

Hvis brukeren eller koden motsier denne skillen, varsle brukeren og spør om skillen og/eller `AGENTS.md` skal oppdateres. Se `AGENTS.md` → "When the user contradicts this documentation".

Bakgrunn om hva som *skal* skje: `postmottak-dokumentflyt` (forretningsflyten) og `postmottak-flyt-motor` (hvordan motoren fungerer).

## Kilder, de billigste først

1. **Drift-API** (Azure AD; rollen `AAP_DRIFT_LES` for lesing, DRIFTE for handlinger):
   - `GET /api/drift/journalpost/{journalpostId}/info`: innkommende status, brevkode, tema, regelresultat, Joark-status, kanal, saksnummer og alle behandlinger med steg, status og historikk for avklaringsbehov. Fødselsnummer er fjernet fra svaret.
   - `POST /api/drift/person/journalposter/søk` med `{ "ident": "..." }`: innkommende journalposter for en person.
   - `POST /api/drift/behandling/{referanse}/prosesser`: legger behandlingen i kø for prosessering. Trygt å gjenta (hvert kall legger inn en ny prosessering, og stegene er skrevet slik at de tåler å kjøres flere ganger), men kallet havner bak en `FEILET` jobb for samme behandling.
   - `POST /api/drift/journalpost/{journalpostId}/kopier-journalpost`: kopierer en journalpost i Joark (bare tema AAP).
2. **Motor-API** (prod: rollen Team AAP): `GET /drift/api/jobb/feilende`, `GET /drift/api/jobb/planlagte-jobber`, `GET /drift/api/jobb/sisteKjørte`, `GET /drift/api/jobb/rekjor/{jobbId}`, `GET /drift/api/jobb/avbryt/{jobbId}`, `GET /drift/api/jobb/rekjorAlleFeilede`, `POST /drift/api/jobb/{jobbId}/kjor` (kjør en `KLAR` jobb nå). `rekjor` og `avbryt` virker bare på `FEILET` jobber.
3. **Flyt-visningen i frontend**: `GET /api/behandling/{referanse}/flyt` returnerer `prosessering.status` (`JOBBER`, `FEILET`, `FERDIG`) og feilteksten fra en feilet jobb.
4. **Logger** (Loki/Elastic): MDC-feltene `journalpostId`, `behandlingReferanse`, `behandlingId`, `sakId` (= journalpostId), `jobbType` og `callId`. Jobbfeil logges som `Feil under prosessering av jobb :: [type] ... <ExceptionClass>. Se secure logs.`. Stacktracen finnes bare i team-logs (secure) og i `jobb_historikk.feilmelding` (SQL «Jobber»). Motorens egne logglinjer er listet i `postmottak-testing/scripts/flyt-trace.sh` (`Behandler steg`, `Stopper opp ved`, `Tilbakeført`, ...).
5. **SQL** (lesetilgang): `references/sql.md`. Ikke hent ut fødselsnummer med mindre du må, og lim dem aldri inn i saker eller chatter.

## Feilsøkingsguide: «hvor er journalpost X / hvorfor kom den ikke til Kelvin?»

| Observasjon | Betydning | Neste steg |
| --- | --- | --- |
| Ingen innkommende journalpost og ingen behandling | Kafka-eventet ble filtrert bort (status er ikke `MOTTATT`, kanal EESSI, tema er ikke AAP), eller jobben `vurderdokument.innkommende` har ikke gått bra | SQL «Jobber» for journalposten. Ellers sjekk Joark-status, tema og kanal i SAF. Gjentatte pod-restarter med stream-feil betyr at Kafka-handleren kaster exception. `JoarkAvstemmer` oppretter en Gosys FDR-oppgave for MOTTATT AAP-journalposter uten regelresultat etter 5 dager. |
| Innkommende `IGNORERT` (`ALLEREDE_JOURNALFØRT` / `UTGÅTT`) | Som tilsiktet | Ingen |
| Innkommende `GOSYS_FDR` (`MANGLER_IDENT` / `ORGNR`) | Som tilsiktet: det ble opprettet en fordelingsoppgave i Gosys | Håndteres i Gosys |
| `EVALUERT`, Fordeling-behandling `AVSLUTTET`, regelresultat ARENA | Rutet til Arena | Sjekk `arena.*`-jobbene (SQL «Jobber»). Fallbackene ender i en Gosys JFR/FDR-oppgave. |
| Søknad forventet i Kelvin (kant-til-kant), men havnet i Arena | Toggelen `KantTilKantV1` er av, eller `ArenaService.harOppbruktRettighet` ga ikke `HarBruktOppForlengelsen`/`VedtattAtIkkeForlenges`. Maksdato 12 uker eller mer etter mottatt dato gir `IkkeRelevantSøknad` (12 uker er midlertidig for MVP av kant-til-kant, tilbake til 26 i del 2). | Loggene «Evaluering av fordeling Kelvin/arena» (`oppbruktRettighetResultat`, `featureToggleTilstand`) og «er 'kant-i-kant'» (tilstandsflagg) for journalpostId, og metrikken `postmottak_kant_i_kant_oppbrukt_rettighet` |
| Journalføring åpen med AVKLAR_TEMA eller AVKLAR_SAK åpen | Venter på en saksbehandler | Ingen |
| Journalføring åpen med `VENT_PA_GOSYS` / `MANUELT_SATT_PÅ_VENT` åpen | På vent til fristen (cron 07:00), en løsning eller et Gosys-/Kafka-event | SQL «På vent». Etter temabytte bort fra AAP blir Gosys-venten stående til fristen (kjent oppførsel). |
| En jobb er `FEILET` | Prosesseringen har stoppet. En feilet `flyt.prosesserBehandling` blokkerer også handlinger fra saksbehandler (`BehandlingUnderProsesseringException`). | Les siste `feilmelding` (SQL «Jobber»). Fiks årsaken, kjør deretter jobben på nytt (motor-API) eller vent på `retryFeilede` (07, 12, 15, 20). `POST .../prosesser` hjelper ikke: den nye jobben havner bak den feilede (SQL «Jobber blokkert av en feilet jobb»). |
| DokumentHåndtering åpen ved DIGITALISER_DOKUMENT / AVKLAR_OVERLEVERING | Venter på en saksbehandler | Ingen |
| DokumentHåndtering `AVSLUTTET` | Sendt til behandlingsflyt hvis overleveringsvurderingen sier det | SQL «Aktive vurderinger», og se deretter i aap-behandlingsflyt |
| Behandling er ikke avsluttet, ingen åpne behov og ingen `KLAR`/`FEILET` jobb | Står fast (skal ikke skje) | SQL «Fastlåste behandlinger», deretter `POST /api/drift/behandling/{ref}/prosesser` |

Fakta som forklarer mange henvendelser:
- `T` i den kompakte steghistorikken viser at et steg ble rullet tilbake, ikke hvorfor. Utløpte ventepunkter og åpne avklaringsbehov i tidligere steg kan også gi tilbakerulling. Logglinja `Tilbakeført etter oppdatering av registeropplysninger fra 'X' til 'Y'` viser at et informasjonskrav som ga `ENDRET` forårsaket tilbakerullingen. Før november 2026 skjedde dette ved nesten hver gjenopptakelse i Journalføring på grunn av en snudd sjekk. Eldre historikk kan derfor vise `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX`. Gjentatte nyere logglinjer av denne typen tyder på et informasjonskrav som melder `ENDRET` hver gang.
- En DIGITALISER_DOKUMENT som saksbehandler har løst, vises med status `AVBRUTT`. Se etter en `AVSLUTTET` endring i historikken.
- Stegene kjøres på nytt etter tilbakerullinger og retry-forsøk, så gjentatte kall til Joark, Gosys eller behandlingsflyt for samme journalpost er forventet.
- Behandlingsstatusen `IVERKSETTES` betyr «i de siste stegene», ikke «ferdig». `AVSLUTTET` er ferdig.
- `system_navn` i regelresultatet er utfallet av reglene. For journalposter med en Fordeling-behandling er beslutningen som ble fulgt `avklar_fordeling_vurdering.fagsystem` (`vurdert_av` `Kelvin` = automatisk, en Nav-ident = manuelt).

## Koder

Kolonnen `definisjon` for avklaringsbehov: `1338` DIGITALISER_DOKUMENT, `1339` AVKLAR_TEMA, `1340` AVKLAR_SAK, `1341` AVKLAR_OVERLEVERING, `1342` VENT_PA_GOSYS, `1343` AVKLAR_FORDELING (fjernet, bare historiske data), `9001` MANUELT_SATT_PÅ_VENT. Jobbtyper: `postmottak-flyt-motor/references/motor-jobs-transactions.md`.

## Referanser

- `references/sql.md`: verifiserte lesende spørringer: journalpostoversikt, kompakt steghistorikk, avklaringsbehov, jobber med feil, fordelingsregler, vurderinger, Kelvin-saker, personsøk, på vent, fastlåste behandlinger, feilede og blokkerte jobber, kø og inntak.
- `RUNBOOK.md` (repo-roten, norsk): driftsoppskrifter skrevet av teamet.
