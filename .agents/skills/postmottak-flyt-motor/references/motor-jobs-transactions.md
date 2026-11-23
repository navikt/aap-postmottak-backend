# Motor, jobber og transaksjoner

Motoren er jobbmotoren fra aap-komponenter (`no.nav.aap.motor`, kildene ligger i Gradle-cachen som `motor-<version>-sources.jar`). Denne filen beskriver hvordan postmottak bruker den. **Verifisert** betyr bekreftet ved en testkjøring.

## Oppsett

- `app/.../App.kt` `lagMotor`: `Motor(dataSource, antallKammer = 4, jobber = ProsesseringsJobber.alle(), repositoryRegistry, gatewayProvider, logInfoProvider = PostmottakLogInfoProvider)`. Tester bruker 2 workere.
- Workerne kjører på virtuelle tråder. Hver av dem looper: plukk én jobb i en «plukk»-transaksjon, kjør den i en **ny** transaksjon, og marker den deretter `FERDIG` eller registrer feilen i plukk-transaksjonen. Ledige workere sover i 500 ms.
- `RetryService(connection).enable()` ved oppstart planlegger alle cron-jobber, inkludert motorens egen `oppgave.retryFeilede`.
- `PostmottakLogInfoProvider` legger `behandlingReferanse` og `journalpostId` i MDC for hver jobb med en behandlingId.

## JobbInput-konvensjoner i postmottak

| Builder | Betydning her |
| --- | --- |
| `forBehandling(journalpostId.referanse, behandlingId.id)` | Jobber som gjelder en behandling. Motorens «sakId» **er journalpostId**. |
| `forSak(journalpostId.referanse)` | Jobber som gjelder en journalpost uten behandling (`VurderRelevantDokumentForAAPJobbUtfører`, alle Arena-jobber). |
| ingen av dem | Frittstående jobber (cron-jobber). |
| `medParameter` / `medPayload` | Properties eller en JSON-streng. Hjelpefunksjoner: `medJournalpostId`/`getJournalpostId` (`prosessering/JobbInputExtensions.kt`), `medArenaVideresenderKontekst` / `medAutomatiskJournalføringKontekst` (setter også `forSak`). |
| `medCallId()` | Kopierer `callId` fra MDC, så loggene kan korreleres. |

Jobbens `type`-streng lagres i `JOBB.type` og parses tilbake gjennom registeret. **Endre aldri en type**, og la jobben stå registrert i `ProsesseringsJobber.alle()` så lenge det kan finnes rader av den typen. Fjernede funksjoner beholder tomme stubber av denne grunnen (`RedigitaliseringKopierJobbUtfører`, `RedigitaliseringBehandlingJobbUtfører`).

## Rekkefølge og parallellitet

- Jobber med sakId eller behandlingId danner en eksklusivitetsgruppe per **(sak_id, behandling_id, type)**. Bare den eldste `KLAR`/`FEILET`-jobben (etter `neste_kjoring`) i en gruppe kan plukkes.
- En `FEILET` jobb øverst i en gruppe **blokkerer** gruppa til noen kjører den på nytt eller avbryter den. For `flyt.prosesserBehandling` blokkerer den også saksbehandlerhandlinger (`BehandlingUnderProsesseringException` sjekker etter `KLAR`- og `FEILET`-jobber).
- Ulike typer for samme behandling, og ulike behandlinger for samme journalpost, er ulike grupper og kan kjøre parallelt. Radlåsen på behandlingen (`TaSkriveLåsRepository`) serialiserer prosesseringen mot API-mutasjoner.
- Jobber som legges i køen inne i en jobb, blir synlige ved commit og kjører derfor etter at den gjeldende jobben er ferdig (**verifisert** i traces: `flyt.hendelse` plukkes etter `flyt.prosesserBehandling` som opprettet den).
- Plukkeren postmottak bruker, ignorerer `Prioritet`: klare jobber kjører i rekkefølgen `neste_kjoring`.
- komponenter har også en andre plukker (`plukkJobbV2`, kolonnen `JOBB.kjorbar`, `skjedulerJobber`) som tar hensyn til `prioritet`, og som slås på med `Motor(enableV2 = ...)`. Postmottak slår den ikke på, så `kjorbar` forblir `false` og reglene over gjelder.
- Motorens cron `jobber.arkiverFerdigstilte` (daglig 01:00) flytter `FERDIG`-jobber der `neste_kjoring` er mer enn 30 dager gammel, til `JOBB_ARKIV`/`JOBB_HISTORIKK_ARKIV`.

## Feil, retries og omkjøringer

- Ved en exception rulles jobbens transaksjon tilbake til siste savepoint (se under) og resten committes. Det legges til en `FEILET`-rad med stacktrace i `JOBB_HISTORIKK`, og jobben forblir `KLAR` til den har feilet `retries` ganger. Da blir den `FEILET`.
- `retries` er **totalt antall forsøk** (standard 3). `antallRetriesForsøkt()` er antall tidligere mislykkede forsøk (0 ved første). Ingen backoff er konfigurert, så et nytt forsøk skjer ved neste plukk.
- Motorens cron `oppgave.retryFeilede` (07:00, 12:00, 15:00, 20:00) setter **alle** `FEILET`-jobber tilbake til `KLAR` uten å nullstille historikken. Neste feil markerer dem `FEILET` igjen med en gang, og `antallRetriesForsøkt()` fortsetter å vokse. Flere Arena-jobber er avhengige av dette for å nå reserveløsningen sin.

| Jobb (klasse) | `type` | retries | Reserveløsning / merknad |
| --- | --- | --- | --- |
| `ProsesserBehandlingJobbUtfører` | `flyt.prosesserBehandling` | 3 | ingen |
| `StoppetHendelseJobbUtfører` | `flyt.hendelse` | 3 | ingen |
| `VurderRelevantDokumentForAAPJobbUtfører` | `vurderdokument.innkommende` | 3 | ingen |
| `GjenopptaBehandlingJobbUtfører` | `batch.gjenopptaBehandlinger` | 3 | cron daglig 07:00 |
| `JoarkAvstemmerJobbUtfører` | `Joark-avstemming` | 3 | cron daglig 03:00 (`navn` og `type` ser ut til å være byttet om) |
| `SendSøknadTilArenaJobbUtfører` | `arena.søknad` | 4 | `antallRetriesForsøkt ≥ 3` → ManuellJournalføring-jobb |
| `OppprettOppgaveIArenaJobbUtfører` | `arena.oppgaveoppretter` | 4 | ingen enhet eller `≥ 2` → ManuellJournalføring-jobb |
| `SendTilArenaKjørelisteBehandling` | `arena.kjøreliste` | 4 | `≥ 2` → ManuellJournalføring-jobb |
| `AutomatiskJournalføringJobbUtfører` | `arena.automatiskjournalføring` | 3 | `≥ 3` → ManuellJournalføring-jobb; kan bare nås etter `FEILET` + retryFeilede |
| `ManuellJournalføringJobbUtfører` | `arena.manuell.journalføring` | 6 | enhet og `< 3` → Gosys JFR-oppgave, ellers FDR-oppgave (fordeling) |
| `Redigitalisering*JobbUtfører` | `redigitalisering.kopier`, `redigitalisering.behandling` | 3 | tomme stubber |

Drift-endepunkter fra `motorApi` (i prod kreves Team AAP-rollen): `GET /drift/api/jobb/feilende`, `GET /drift/api/jobb/planlagte-jobber`, `GET /drift/api/jobb/sisteKjørte`, `GET /drift/api/jobb/rekjor/{jobbId}`, `GET /drift/api/jobb/avbryt/{jobbId}`, `GET /drift/api/jobb/rekjorAlleFeilede`, `POST /drift/api/jobb/{jobbId}/kjor`.

## Savepoints: feil består delvis

- `StegOrkestrator.utfør` kaller `behandlingRepository.markerSavepoint()` etter START-fasen og etter OPPDATER_FAKTAGRUNNLAG-fasen i hvert steg. `DBConnection` husker bare det siste savepointet.
- Ved en exception kaller komponenters `DBTransaction` `DBConnection.rollback()`, som ruller tilbake **til savepointet** hvis det finnes ett. Deretter setter `finally` `autoCommit = true`, og det gjør at JDBC **committer** resten av transaksjonen.
- Resultat: alt som er skrevet før siste savepoint, committes: framdrift gjennom tidligere steg, vurderingene og steg_historikk deres, jobber de la i køen (nye behandlinger, hendelser), og faktagrunnlag-oppdateringen til steget som feiler. Bare arbeid etter savepointet går tapt. Neste forsøk gjenopptar ved steget som feilet.
- **Verifisert:** en DokumentHåndtering-behandling der DIGITALISER_DOKUMENT-steget kastet exception, beholdt START_BEHANDLING som fullført, hadde `DIGITALISER_DOKUMENT:START/OPPDATER_FAKTAGRUNNLAG`-rader fra hvert forsøk, og jobben ble `FEILET` etter 3 forsøk.
- Kall til andre systemer som er gjort før exceptionen, rulles ikke tilbake. Sammen med retries betyr det at steg kan gjenta eksterne kall.
- API-forespørsler og Kafka-handleren setter ingen savepoints: de er alt-eller-ingenting.

## Låser og versjon

`TaSkriveLåsRepository.lås` = `SELECT versjon ... FOR UPDATE`; `verifiserSkrivelås` = `UPDATE ... SET versjon = versjon + 1 WHERE versjon = old` (krever nøyaktig én rad). Tas av `ProsesserBehandlingJobbUtfører`, `løs-behov`, `sett-på-vent` og `GET /forbered` (som øker versjon selv når den ikke legger noe i køen). Frontend sender `behandlingVersjon` med mutasjoner. En utdatert verdi gir `OutdatedBehandlingException`.

## Sjekkliste for idempotens i steg og jobber

- Start `utfør()` med å lese den eksisterende vurderingen, og returner `Fullført` hvis den allerede er avgjort.
- Foretrekk oppslag og «opprett hvis det mangler»-kall til andre systemer: `GosysOppgaveGateway.*HvisIkkeEksisterer`, `BehandlingsflytGateway.finnEllerOpprettSak`.
- Joark-oppdateringer (`førJournalpostPåFagsak`, `ferdigstill...`) og `BehandlingsflytGateway.sendHendelse` kan gjentas ved retry. Mottakerne må tåle det.
- Anta aldri én kjøring per behandling: tilbakeføringer, gjenopptakelser og retries kjører steg på nytt (avsnittet «Informasjonskrav-tilbakeføringen i detalj» i `engine-walkthrough.md`).

## Kafka-strømmen er ikke motoren

`JoarkKafkaHandler` kjører på Kafka Streams-tråder med exactly-once-prosessering. Hver record bruker sine egne `datasource.transaction`-blokker. En uhåndtert exception stenger strømklienten (`ProcessingExceptionHandler` → `SHUTDOWN_CLIENT`), `/actuator/live` returnerer 503 og Kubernetes starter poden på nytt. Recorden prosesseres på nytt etter omstart. Hold handlerlogikken tynn: slå opp ting og legg deretter jobber i køen.

## Vente på jobber i tester

`TestUtil(dataSource, cronTypes).ventPåSvar(sakId?, behandlingId?, maxTid = 20)` poller til ingen jobb som matcher filteret, er i en annen tilstand enn `FERDIG`/`FEILET`, og ignorerer cron-typene du sender inn. Den feiler **ikke** ved timeout (den logger bare `Avbryter venting nå`) og behandler `FEILET` som ferdig. Når en flyttest feiler merkelig, sjekk først etter `FEILET`-jobber og `JOBB_HISTORIKK.feilmelding`.
