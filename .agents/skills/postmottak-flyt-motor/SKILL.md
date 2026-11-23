---
name: postmottak-flyt-motor
description: Hvordan motoren i aap-postmottak-backend faktisk fungerer - jobbmotoren (Motor, JobbInput, ProsesserBehandlingJobbUtfører, retries, savepoints) og flyt-tilstandsmaskinen (FlytOrkestrator, StegOrkestrator, BehandlingFlyt, StegStatus, avklaringsbehov, ventepunkter, tilbakeføring). Last før du endrer eller feilsøker steg, flyter, avklaringsbehov, løsere, jobber eller informasjonskrav, eller når en behandling havner i uventet steg, status eller vent.
---

# Postmottak: flytmotor og jobbmotor

Hvis brukeren eller koden motsier denne skillen, si fra til brukeren og spør om skillen og/eller `AGENTS.md` skal oppdateres. Se `AGENTS.md` → "When the user contradicts this documentation".

To ulike motorer som lett blandes sammen:

| Begrep | Hva det er | Hvor |
| --- | --- | --- |
| **motor** | Generisk jobbmotor fra aap-komponenter (`no.nav.aap.motor`). Poller `JOBB`-tabellen, kjører én `JobbUtfører` per DB-transaksjon, prøver på nytt og kjører cron-jobber. | Bibliotek. Postmottak-jobbene registreres i `flyt/.../prosessering/ProsesseringsJobber.kt`. |
| **flyt** (flytmotor) | Postmottaks tilstandsmaskin som flytter én `Behandling` gjennom stegene i flyten sin. | `flyt/.../postmottak/flyt/` (`FlytOrkestrator`, `BehandlingFlyt`), `flyt/.../flyt/steg/` (`StegOrkestrator`). |

Flytmotoren kjører steg **bare** i motor-jobben `ProsesserBehandlingJobbUtfører` (type `flyt.prosesserBehandling`). API-kall kjører aldri steg: de lagrer en beslutning og legger jobben i køen.

## Mental modell

```
hva som helst (Kafka, API, et annet steg, cron)
  └─ FlytJobbRepository.leggTil(JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId, behandlingId))
motoren plukker jobben → ny DB-transaksjon → ProsesserBehandlingJobbUtfører.utfør
  ├─ TaSkriveLåsRepository.lås(behandlingId)          SELECT versjon ... FOR UPDATE
  ├─ FlytOrkestrator.forberedOgProsesserBehandling
  │    forberedBehandling   løs ventepunkter (frist eller evaluerer) → tilbakefør til tidligste åpne behov → oppdater informasjonskrav
  │                         for alle TIDLIGERE steg → tilbakefør hvis oppdateringen tilsier det
  │    prosesserBehandling  for steg i flyten, fra og med behandling.aktivtSteg():
  │                           StegOrkestrator.utfør(steg): START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER
  │                           AVKLARINGSPUNKT finner et åpent avklaringsbehov for dette steget → stopp, send hendelse, returner
  │                         etter siste steg: status AVSLUTTET, send hendelse
  └─ verifiserSkrivelås                               versjon = versjon + 1
commit. Ved exception: commit alt fram til siste savepoint, marker jobben som feilet, prøv på nytt senere.
```

## Flytene

| TypeBehandling | Steg (`StegType`) | Informasjonskrav (oppdateres i OPPDATER_FAKTAGRUNNLAG) |
| --- | --- | --- |
| `Fordeling` | START_BEHANDLING → AVKLAR_FORDELING → AVKLAR_FORDELING_VIDERESEND → IVERKSETTES | AVKLAR_FORDELING: `JournalpostInformasjonskrav` |
| `Journalføring` | START_BEHANDLING → AVKLAR_TEMA → AVKLAR_SAK → SETT_FAGSAK → ENDELIG_JOURNALFØRING → VIDERESEND → IVERKSETTES | AVKLAR_TEMA: `JournalpostInformasjonskrav`, `SaksnummerInfoKrav` |
| `DokumentHåndtering` | START_BEHANDLING → DIGITALISER_DOKUMENT → OVERLEVER_TIL_FAGSYSTEM → IVERKSETTES | ingen |
| `Under18` | START_BEHANDLING → AVKLAR_UNDER18 → AVKLAR_UNDER18_SAMTYKKE → OPPRETT_UNDER18_SAK → IVERKSETTES | AVKLAR_UNDER18: `JournalpostInformasjonskrav`, `Under18FødselsdatoInformasjonskrav` |

Definert i `flyt/.../forretningsflyt/behandlingstyper/` og koblet i `flyt/.../flyt/BehandlingType.kt` (`utledType`). `IVERKSETTES` er `AvsluttBehandlingSteg` (gjør ingenting). Under18-flyten er ikke klar for produksjon: den registrerte `Under18RepositoryImpl` har `TODO()` i metodene for under 18, og testene bruker `InMemoryUnder18Repository`. Det forretningsmessige formålet med de eksisterende flytene står i skillen `postmottak-dokumentflyt`.

## Regler som ikke er åpenbare når du leser én fil

1. **Et steg stopper aldri flyten selv.** `utfør()` returnerer `Fullført` eller `FantAvklaringsbehov`. Motoren registrerer behovet i UTFØRER og stopper i neste fase, AVKLARINGSPUNKT, hvis et *åpent* behov hører til dette steget. `FunnetAvklaringsbehov.kanFortsette()` er `true`; bare `Stopp` (fra AVKLARINGSPUNKT) er `false`.
2. **`utfør()` kjører på nytt fra START hver gang jobben gjenopptas.** Et steg som har reist et behov, kjøres minst to ganger: én gang for å oppdage behovet og én gang etter at løseren har lagret svaret. Alle steg må starte med «finnes det allerede en vurdering? Da returnerer vi `Fullført`». Det er også derfor steg må tåle å kjøres om igjen etter tilbakeføringer og retries.
3. **Løsere lagrer svaret, steg handler på det.** `AvklaringsbehovLøsning.løs()` → `*Løser` skriver en vurdering i API-transaksjonen. Steget leser den ved neste kjøring. Sideeffekter mot andre systemer hører hjemme i steg, med noen få unntak (`AvklarSakLøser` kaller behandlingsflyt `finnEllerOpprettSak`, `AvklarTemaLøser` setter eller fjerner Gosys-vent).
4. **En informasjonskrav-drevet tilbakeføring skjer når et informasjonskrav i et tidligere steg returnerer `ENDRET`.** `InformasjonskravGrunnlagImpl` returnerer de endrede informasjonskravene, og `FlytOrkestrator.forberedBehandling` tilbakefører til det tidligste steget som eier ett av dem (samme semantikk som aap-behandlingsflyt). Loggmeldingen er `Tilbakeført etter oppdatering av registeropplysninger fra 'X' til 'Y'`. En tilbakeføring kan også skje av andre grunner, for eksempel et utløpt ventepunkt eller et åpent avklaringsbehov i et tidligere steg. Et informasjonskrav som feilaktig melder `ENDRET` (for eksempel fordi lagrede og hentede data aldri er like) gir en informasjonskrav-drevet tilbakeføring ved hver gjenopptakelse. Før november 2026 var resultatet snudd (`filterNot`), så gammel steghistorikk i Journalføring viser ofte `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX`.
5. **Tilbakeføring kjører ingen stegkode.** Den logger bare `TILBAKEFØRT`-rader for hvert steg på veien tilbake og flytter markøren. Vurderinger beholdes. Den sender også en `DokumentflytStoppetHendelse`, selv om prosesseringen fortsetter rett etterpå.
6. **Hvert steg konstrueres fem ganger per gjennomkjøring** (`stegKonstruktør.konstruer` én gang per fase). `konstruer()` og konstruktører må være billige og uten sideeffekter.
7. **Savepoints gjør at feil delvis består.** `StegOrkestrator` setter et savepoint etter START og etter OPPDATER_FAKTAGRUNNLAG. Når en senere fase kaster exception, rulles transaksjonen tilbake til savepointet og committes deretter (`DBConnection.rollback` + `autoCommit = true` i komponenter). Tidligere steg og faktagrunnlag-oppdateringen til steget som feiler, lagres. Jobben prøver på nytt fra steget som feilet, og markeres `FEILET` etter 3 forsøk. Verifisert.
8. **Eksterne kall i steg kan gjentas** (retries, tilbakeføringer, gjenopptakelser). Bruk idempotente operasjoner: `...HvisIkkeEksisterer` for Gosys, `finnEllerOpprettSak` og mottakere som dedupliserer på journalpost.
9. **Ventepunkter lukkes bare på disse måtene:** i `forberedBehandling` gjennom `VentebehovEvaluererService` (`flyt/ventebehov/`, samme utforming som i aap-behandlingsflyt) når fristen har utløpt eller en `SpesifikkVentebehovEvaluerer` for definisjonen sier at behovet er løst. `Under18SamtykkeVurderingVentebehovEvaluerer` lukker samtykkevent når den aktive vurderingen sier at begge foreldrene har samtykket. Evaluererne kjører bare når noe legger prosessering i køen, for eksempel en løsning eller cron `GjenopptaBehandlingJobbUtfører` kl. 07:00 for utløpte frister. Andre måter er at saksbehandler løser `MANUELT_SATT_PÅ_VENT`, `AvklaringsbehovOrkestrator.taAvVentPgaGosys`, eller at et steg returnerer `Fullført` mens behandlingen *ikke* har noe avklaringsbehov som ikke er vent (uansett status). Et steg som fullføres, lukker **ikke** vent hvis for eksempel `AVKLAR_TEMA` finnes. Verifisert: `VENT_PA_GOSYS` blir stående åpent etter at journalpostens tema er endret bort fra AAP.
10. **`Avklaringsbehovene.avbrytForSteg` ignorerer status.** `DigitaliserDokumentSteg` kaller den når det finnes en vurdering, så en `DIGITALISER_DOKUMENT` som saksbehandler har løst, ender som `AVBRUTT` (historikk `OPPRETTET, AVSLUTTET, AVBRUTT`). Verifisert. Bruk `hvemSomLøste()` (siste `AVSLUTTET`) for å finne hvem som løste den.
11. **`Status.erAvsluttet()` er sann for både `IVERKSETTES` og `AVSLUTTET`.** En behandling er `IVERKSETTES` mens den er i VIDERESEND, AVKLAR_FORDELING_VIDERESEND eller IVERKSETTES, og `AVSLUTTET` først etter siste steg. `hentÅpenJournalføringsbehandling` matcher bare `OPPRETTET`/`UTREDES`.
12. **Motorens «sakId» er journalpostId.** `forBehandling(journalpostId, behandlingId)`. Jobber kjører strengt i rekkefølge per (sakId, behandlingId, type). En `FEILET` jobb blokkerer senere jobber av samme type i den gruppa, og blokkerer saksbehandlerhandlinger på behandlingen (`BehandlingUnderProsesseringException`).
13. **Dataomfang:** journalpostdata lagres per journalpostId og deles av alle behandlinger for den journalposten. Vurderinger er per behandling og kopieres Journalføring → DokumentHåndtering av `GrunnlagKopierer` (bare repositories som overstyrer `kopier`). Fordeling → Journalføring kopierer ingenting. Dokumentflyt har ingen informasjonskrav og feiler hvis ingen journalpost er lagret av en tidligere behandling. Verifisert.

## Hvor ting ligger

| Tema | Fil (under `flyt/src/main/kotlin/no/nav/aap/postmottak/`) |
| --- | --- |
| Inngangspunkt for jobb | `prosessering/ProsesserBehandlingJobbUtfører.kt` |
| Flyt-løkke, tilbakeføringer | `flyt/FlytOrkestrator.kt`, `flyt/BehandlingFlyt.kt` |
| Stegfaser, savepoints | `flyt/steg/StegOrkestrator.kt`, `journalpostogbehandling/flyt/StegStatus.kt` |
| Stegresultater | `flyt/steg/{Fullført,FantAvklaringsbehov,Transisjon}.kt` |
| Avklaringsbehov-modell | `avklaringsbehov/{Avklaringsbehovene,Avklaringsbehov,AvklaringsbehovService}.kt`, `kontrakt/.../avklaringsbehov/Definisjon.kt` |
| Løse behov (API-siden) | `avklaringsbehov/AvklaringsbehovOrkestrator.kt`, `avklaringsbehov/løsning/*`, `avklaringsbehov/løser/*` |
| Behandling, status, steghistorikk | `journalpostogbehandling/behandling/{Behandling,StegTilstand}.kt`, `repository/.../BehandlingRepositoryImpl.kt` |
| Jobbregister | `prosessering/ProsesseringsJobber.kt` |
| Utgående hendelse til oppgave/statistikk | `hendelse/avløp/BehandlingHendelseServiceImpl.kt` → `prosessering/StoppetHendelseJobbUtfører.kt` |

## Før du endrer motorkode

- Les referansen for området først. De fleste «bugene» her er tilsiktede eller bærende.
- `BehandlingFlyt` er synkronisert fra aap-behandlingsflyt (KDoc sier det). Hold diffene små, så det er enklere å synkronisere senere.
- Mens du itererer, kjør målrettede enhetstester og **én eller to** relevante `Flyttest`-metoder. Kjør hele suiten før du avslutter (se `postmottak-testing`).
- Bruk `../postmottak-testing/scripts/flyt-trace.sh` for å lese hva motoren gjorde, i stedet for å lese rå logger.

## Referanser (les ved behov)

- `references/engine-walkthrough.md`: kommentert algoritme for FlytOrkestrator, StegOrkestrator og BehandlingFlyt, alle jobbtriggere, status og steg_historikk, en kommentert reell trace.
- `references/avklaringsbehov.md`: alle definisjoner, livssyklus, de to måtene steg oppretter behov på, løsningsflyten fra ende til ende, ventepunkter, valideringer.
- `references/motor-jobs-transactions.md`: motorsemantikk, eksklusivitetsgrupper, retries og omkjøringer, savepoints, låser, cron-jobber, idempotens.
- `references/recipes.md`: hvordan du legger til steg, avklaringsbehov, informasjonskrav, vurderingsrepository, flyt, jobb eller gateway, og hva du må passe på for behandlinger som er under arbeid.
