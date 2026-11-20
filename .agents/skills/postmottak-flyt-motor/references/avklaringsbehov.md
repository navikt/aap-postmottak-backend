# Avklaringsbehov: definisjoner, livssyklus, løsningsflyt, ventepunkter

Stier ligger under `flyt/src/main/kotlin/no/nav/aap/postmottak/` med mindre noe annet er oppgitt. **Verifisert** betyr bekreftet ved en testkjøring.

## Definisjoner

`kontrakt/.../avklaringsbehov/Definisjon.kt` (publisert kontrakt, brukt av frontend og andre apper). Kodene er duplisert som `const val *_KODE` og `enum AvklaringsbehovKode` i `AvklaringsbehovKode.kt`. Hold dem synkronisert.

| Definisjon | Kode | Type | Løses i steg | Opprettes av | Løsning → Løser |
| --- | --- | --- | --- | --- | --- |
| AVKLAR_FORDELING (utfaset og fjernet, bare historiske data) | 1343 | MANUELT_PÅKREVD | AVKLAR_FORDELING | reises aldri | ingen (løsning og løser er fjernet) |
| AVKLAR_TEMA | 1339 | MANUELT_PÅKREVD | AVKLAR_TEMA | `AvklarTemaSteg` via `AvklaringsbehovService` | `AvklarTemaLøsning` → `AvklarTemaLøser` |
| AVKLAR_SAK | 1340 | MANUELT_PÅKREVD | AVKLAR_SAK | `AvklarSakSteg` returnerer `FantAvklaringsbehov` | `AvklarSaksnummerLøsning` → `AvklarSakLøser` |
| DIGITALISER_DOKUMENT | 1338 | MANUELT_PÅKREVD | DIGITALISER_DOKUMENT | `DigitaliserDokumentSteg` | `DigitaliserDokumentLøsning` → `DigitaliserDokumentLøser` |
| AVKLAR_OVERLEVERING | 1341 | MANUELT_PÅKREVD | OVERLEVER_TIL_FAGSYSTEM | `OverleverTilFagsystemSteg` | `AvklarOverleveringLøsning` → `AvklarOverleveringLøser` |
| MANUELT_SATT_PÅ_VENT | 9001 | VENTEPUNKT, standardfrist 3 uker | UDEFINERT = steget den ble satt i | API `sett-på-vent` | `SattPåVentLøsning` → `SattPåVentLøser` (gjør ingenting, «Tatt av vent») |
| VENT_PA_GOSYS | 1342 | VENTEPUNKT, standardfrist 5 dager, men satt med +2 uker | AVKLAR_TEMA | `AvklarTemaLøser` → `AvklaringsbehovOrkestrator.settBehandlingPåVentForTemaEndring` | ingen: kan ikke løses via API-et |

Alle løses av rollen `SAKSBEHANDLER_NASJONAL`, og ingen krever totrinnsgodkjenning. Koden `1337` finnes i `AvklaringsbehovKode`, men har ingen definisjon (legacy).

## Statuser og lagring

- `AVKLARINGSBEHOV`: én rad per (behandling, definisjon) (unik indeks), med `funnet_i_steg`.
- `AVKLARINGSBEHOV_ENDRING`: historikk som bare utvides (status, begrunnelse, frist, venteårsak, opprettet_av, tid). Gjeldende status = siste endring etter tidsstempel.
- Åpen: `OPPRETTET` (og den ubrukte `SENDT_TILBAKE_FRA_BESLUTTER`). Lukket: `AVSLUTTET` (løst) eller `AVBRUTT` (kansellert).
- `Avklaringsbehovene` (fra `AvklaringsbehovRepository.hentAvklaringsbehovene`) leser databasen ved **hvert** kall (`alle()`, `åpne()` ...) og returnerer nye objekter. Endre tilstand bare gjennom metodene, som lagrer (`leggTil`, `løsAvklaringsbehov`, `avbrytForSteg`, ...).

Tilstandsoverganger:

| Fra → til | Forårsaket av |
| --- | --- |
| ingen → OPPRETTET | `leggTil`: et steg returnerte `FantAvklaringsbehov`, `AvklaringsbehovService` eller sett-på-vent |
| OPPRETTET → AVSLUTTET | `løsAvklaringsbehov`: API-løsning, utløpt frist, `taAvVentPgaGosys`, regelen for lukking av vent under; `internalAvslutt` fra `AvklaringsbehovService` |
| OPPRETTET eller AVSLUTTET → AVBRUTT | `avbrytForSteg` (ignorerer status), `internalAvbryt` fra `AvklaringsbehovService` |
| AVSLUTTET eller AVBRUTT → OPPRETTET | `leggTil` gjenåpner (for ventepunkter oppdaterer den også `funnet_i_steg`) |

## Hvordan steg oppretter behov

**Eksplisitt (de fleste steg):** `return FantAvklaringsbehov(Definisjon.X)`. `StegOrkestrator` kaller `leggTil(X, gjeldendeSteg)`: opprett hvis det mangler, gjenåpne hvis lukket, ignorer hvis allerede åpent. Steget må returnere `Fullført` så snart en vurdering finnes, ellers gjenåpner det behovet i det uendelige.

**Deklarativt (bare `AvklarTemaSteg`):** `AvklaringsbehovService.oppdaterAvklaringsbehov(def, vedtakBehøverVurdering, erTilstrekkeligVurdert, kontekst)` setter ønsket tilstand, deretter returnerer steget `Fullført`. AVKLARINGSPUNKT stopper hvis behovet er åpent.

| trenger vurdering? | gjeldende tilstand | tilstrekkelig vurdert? | Resultat |
| --- | --- | --- | --- |
| ja | ingen, `AVBRUTT` eller aldri løst | (ikke spurt) | OPPRETTET (opprett/gjenåpne) |
| ja | løst tidligere og nå `AVSLUTTET` | ja | uendret |
| ja | løst tidligere, nå `OPPRETTET` | ja | `AVSLUTTET` (`internalAvslutt`) |
| ja | løst tidligere | nei | gjenåpne → OPPRETTET |
| nei | ingen eller `AVBRUTT` | (ikke spurt) | uendret |
| nei | `OPPRETTET`/`AVSLUTTET` | (ikke spurt) | `AVBRUTT` |

For AVKLAR_TEMA er «trenger vurdering» false for ugyldig, allerede JOURNALFOERT, tema ≠ AAP, digital søknad/legeerklæring/meldekort og automatisk klage. «Tilstrekkelig vurdert» er bare `false` når vurderingen er UKJENT mens Joark fortsatt sier AAP og toggelen `PostmottakVelgTema` er av. Derfor gjenåpnes AVKLAR_TEMA etter «skal ikke til AAP» (**verifisert**).

## Løsningsflyten fra ende til ende

1. `POST /api/behandling/løs-behov` med `{ referanse, behandlingVersjon, behov: { behovstype: "1339", ... } }` (`api/.../flyt/AvklaringsbehovApi.kt`). `behovstype` velger subtypen av `AvklaringsbehovLøsning` gjennom `@JsonTypeName`. Subtypene registreres i `App.kt` med `registerSubtypes(utledSubtypes())`. Autorisasjon: operasjon SAKSBEHANDLE på journalposten, rolle fra `definisjon.løsesAv`.
2. I én transaksjon: lås behandlingsraden, deretter `BehandlingTilstandValidator`: status ≠ AVSLUTTET, `behandlingVersjon` stemmer (`OutdatedBehandlingException`), og ingen `flyt.prosesserBehandling`-jobb i `KLAR` **eller `FEILET`** for behandlingen (`BehandlingUnderProsesseringException`).
3. `AvklaringsbehovHendelseHåndterer` → `ValiderBehandlingTilstand`: behovet må finnes på behandlingen (uansett status), og steget må ikke ligge etter det gjeldende steget.
4. `AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering`:
   1. `flytOrkestrator.forberedLøsingAvBehov(def)`: tilbakefør til steget der behovet løses (ingen hendelse sendes), og krev deretter at markøren står nøyaktig der. Å løse et tidligere behov på nytt (for eksempel AVKLAR_TEMA mens vi står i AVKLAR_SAK) er derfor tillatt og tilbakefører behandlingen.
   2. `løsning.løs(...)` → løseren lagrer vurderingen (kan kalle andre systemer, for eksempel behandlingsflyt).
   3. `avklaringsbehovene.løsAvklaringsbehov(def, begrunnelse, bruker)` → `AVSLUTTET`.
   4. Legg `ProsesserBehandlingJobbUtfører` i køen.
5. Øk versjon, commit, svar `202 Accepted`. Frontend poller `GET /api/behandling/{ref}/flyt` (prosesseringsstatus `JOBBER`/`FEILET`/`FERDIG` basert på ventende jobber).

En løser som kaster exception, ruller alt tilbake. Ugyldig input må gi `UgyldigForespørselException`, som `StatusPagesConfigHelper` returnerer som 400 med meldingen fra unntaket. Alt annet, inkludert `IllegalArgumentException` fra `require`, blir 500 «En ukjent feil oppsto». Eldre løsere (`AvklarTemaLøser`, `AvklarSakLøser`) bruker fortsatt `require`. Ikke kopier det mønsteret.

## Ventepunkter

| | MANUELT_SATT_PÅ_VENT | VENT_PA_GOSYS |
| --- | --- | --- |
| Settes av | `POST /api/behandling/{ref}/sett-på-vent` → `settBehandlingPåVent` (frist fra forespørselen, ellers +3 uker) | `AvklarTemaLøser` når tema ≠ AAP og `PostmottakVelgTema` er av (frist +2 uker, grunn `VENTER_PÅ_BEHANDLING_I_GOSYS`) |
| Hører til steg | behandlingens gjeldende steg (`funnet_i_steg`) | AVKLAR_TEMA |
| Starter prosessering? | nei (bare en hendelse) | nei; løsningsjobben gjør det, og stopper igjen ved AVKLAR_TEMA |
| Fjernes av | løsning `SattPåVentLøsning` (behovstype 9001), eller frist | `taAvVentPgaGosys` (AvklarTemaLøser med AAP eller med toggelen på; Kafka AAP-hendelse for en journalpost med åpen Journalføring-behandling), eller frist |

Felles regler:
- Utløp av frist: cron `GjenopptaBehandlingJobbUtfører` (07:00) legger prosessering i køen for `UTREDES`-behandlinger der behovet har siste endring `OPPRETTET` med `frist <= i dag`. `FlytOrkestrator.forberedBehandling` løser deretter utløpte ventepunkter som SYSTEMBRUKER gjennom `VentebehovEvaluererService` og tilbakefører til steget deres.
- Hendelsesbasert oppheving av vent: registrer en `SpesifikkVentebehovEvaluerer` i `RepositoryProvider`-konstruktøren til `VentebehovEvaluererServiceImpl`, uten refleksjon. `Under18SamtykkeVentebehovEvaluerer` bruker mottakshistorikken og siste `OPPRETTET`-tidspunkt for venten. Dermed kan ikke samme dokument ta behandlingen av vent på nytt i en senere venteperiode. Triggeren legger bare `ProsesserBehandlingJobbUtfører` i køen. Evalueringen skjer under prosessering. Alle evaluerere konstrueres ved hver prosessering av en behandling på vent. Bruk bare repositorier som er registrert i produksjon.
- Et steg som returnerer `Fullført`, lukker åpne ventepunkter **bare hvis behandlingen ikke har noe avklaringsbehov som ikke er vent, uansett status**. **Verifisert konsekvens:** etter at Gosys flytter journalposten til et annet tema, blir AVKLAR_TEMA `AVBRUTT`, men `VENT_PA_GOSYS` står åpent til fristen, så Journalføring-behandlingen venter opptil to uker før den blir ferdig.
- `AVKLARINGSPUNKT` stopper på et åpent ventepunkt i gjeldende steg på lik linje med alle andre behov.
- Å være på vent endrer ikke behandlingsstatus.
- Bare tre `ÅrsakTilSettPåVent`-verdier fungerer fra ende til ende: `VENTER_PÅ_OPPLYSNINGER`, `VENTER_PÅ_SVAR_FRA_BRUKER`, `VENTER_PÅ_BEHANDLING_I_GOSYS`. API-et og kontrakten godtar seks, men `BehandlingHendelseServiceImpl` kaster `error("Skal ikke kunne skje")` for de andre når den bygger hendelsen, og det får forespørselen til å feile. Oppdater den mappingen når du legger til eller aktiverer en årsak.

## Utgående hendelse (`DokumentflytStoppetHendelse`)

`BehandlingHendelseServiceImpl.stoppet` legger `StoppetHendelseJobbUtfører` (`flyt.hendelse`) i køen. Den sender hendelsen til aap-oppgave (`OppgaveGateway.varsleHendelse`, oppretter og lukker Kelvin-oppgaver) og aap-statistikk. Den sendes når prosesseringen stopper eller avsluttes, ved hver tilbakeføring fra `forberedBehandling`, ved sett-på-vent og ved hver kjøring av `AvklarTemaLøser`. Mottakerne må derfor tåle gjentatte hendelser med samme tilstand.

- `aktivtAvklaringsbehov`: første behov som ikke er vent (sortert etter flytrekkefølgen til `funnetISteg`) og som er åpent i gjeldende steg.
- `avklaringsbehov`: alle behov med full historikk.
- Hvis det verken finnes en journalpost eller noen behov for behandlingen, sendes ingenting (warn-logg). Hvis det finnes behov, men ingen journalpost, kastes en exception.

## Andre ting som overrasker

- `avbrytForSteg(steg)` kansellerer **hvert** behov i det steget som ikke er vent, uansett status. `DigitaliserDokumentSteg` kaller den hver gang det finnes en vurdering, så en DIGITALISER_DOKUMENT som saksbehandler har løst, ender som `AVBRUTT` (**verifisert**: historikk `OPPRETTET, AVSLUTTET, AVBRUTT`). `OverleverTilFagsystemSteg` kansellerer DIGITALISER_DOKUMENT (ikke sitt eget steg) for ugyldige journalposter.
- `hvemSomLøste(def)` returnerer `endretAv` fra siste `AVSLUTTET`-endring. `SettFagsakSteg` og `JournalføringSteg` sender den til Joark som `Nav-User-Id` (hvem som journalførte).
- `leggTil` på et eksisterende åpent behov gjør ingenting (logger «Forsøkte å legge til et avklaringsbehov som allerede eksisterte»).
