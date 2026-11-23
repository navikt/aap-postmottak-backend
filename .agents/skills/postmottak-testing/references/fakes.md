# Fakes og testdatakatalog

Alt her ligger i `lib-test/src/main/kotlin/no/nav/aap/postmottak/test/` med mindre noe annet er oppgitt. Fake-tilstand er global JVM-tilstand som aldri tømmes: opprett ferske data per test gjennom byggerne.

## Fake HTTP-servere (`FakeServers.kt`, startes av `@Fakes`)

`@Fakes` (`FakesExtension`) starter én Netty-server per system på en tilfeldig port og setter de tilhørende systemegenskapene `INTEGRASJON_*`, `NAIS_CLUSTER_NAME=LOCAL` og token-endepunkter. Bruk dem gjennom de ekte klientene: `defaultGatewayProvider { register<FakeUnleash>() }`.

| System | Oppførsel | Styr den med |
| --- | --- | --- |
| Texas (tokens) | `/token` utsteder en JWT i Azure-stil (azp `postmottak`, eller `INTEGRASJON_POSTMOTTAK_AZP`). `/token/exchange` utsteder et OBO-token som beholder `NAVident`. `/introspect` sier aktiv | ingenting |
| SAF GraphQL (`fakes/Saf.kt`) | `journalpost(id)` bygget fra `TestJournalposter`. En ukjent id får en standard `TestJournalPost` (digital legeerklæring). `DATO_REGISTRERT` er alltid 2020-12-01T10:00. `saker` returnerer en tom liste. | `TestJournalposter.leggTil { }` |
| SAF REST | `hentdokument`: JSON-en `digitalSøknad` for JSON-varianter, ellers `sample.pdf` | `digitalSøknad = SøknadV0(...)` på byggeren |
| dokarkiv (Joark) | PUT journalpost (godtar en oppdater-request, eller en endre-tema-request med bare `tema`), PATCH ferdigstill. Ingenting registreres. | ingenting |
| aap-behandlingsflyt (`fakes/BehandlingsflytFake.kt`) | `finnEllerOpprett`: personens første Kelvin-sak, ellers en tilfeldig ny. `ekstern/finn`: den registrerte personens `kelvinsaker` (tom hvis ingen). **En uregistrert ident får én tilfeldig sak**. `hendelse/send`: 204. `finnBehandlingerAvType`: to klagebehandlinger | `TestPersoner.leggTil { kelvinSak = TestKelvinSak(...) }` |
| aap-oppgave, aap-statistikk | godtar alt | ingenting |
| tilgang | gir alltid tilgang | ingenting |
| Gosys oppgave-API (`fakes/GosysoppgaveFake.kt`) | GET returnerer én oppgave bare hvis journalposten har `harEksisterendeGosysOppgave`. POST kaster `ShouldNotBeCalledException` i så fall. PATCH ok | `harEksisterendeGosysOppgave = true` |
| aap-arenaoppslag (`fakes/ArenaoppslagFake.kt`) | historikk og signifikant historikk = `TestPerson.harHistorikkIArena`. Maksdato og siste utbetaling fra `TestPerson.arenaSak` (404 hvis ingen) | `harHistorikkIArena`, `arenaSak = TestArenaSak(...)` |
| aap-fss-proxy (Arena WS) | `nyesteaktivesak`: `"1234"` hvis `harAktivSakIArena`, ellers 204 (styrer også `harAktivSak`). `opprettoppgave`: legger requesten til i `FssOppgaver.oppgaver[ident]` og returnerer sak `SAK-5678`. Kjøreliste: sak `"1234"` | `aktivSakIArena`, assert på `FssOppgaver.oppgaver` |
| PDL | identer: den forespurte identen (+ en AKTORID som klienten filtrerer bort). Fødselsdato er **alltid 1990-01-01**. Geografisk tilknytning kommune 3207, ugradert | ingenting |
| NOM (skjermet) | `TestPerson.erSkjermet`. **404 for ukjente personer**, som får klienten til å kaste exception | `skjermet = true` |
| NORG2 | arbeidsfordeling alltid enhet `"superNav!"`. Aktive enheter bare `"9999"`, så hver journalførende enhet regnes som inaktiv | ingenting |
| veilarbarena | oppfølgingsenhet `"9999"` | ingenting |
| EREG | tomt svar | ingenting |
| Unleash-server | **alle** `PostmottakFeature` påslått (brukes bare hvis `FakeUnleash` ikke er registrert) | registrer `FakeUnleash` eller din egen |

## Byggere for testdata

`TestPersoner.leggTil { }` (`modell/TestPerson.kt`) returnerer en `TestPerson` på 19 år med et generert gyldig fnr:

| Felt | Effekt |
| --- | --- |
| `kelvinSak = TestKelvinSak(saksnummer, periode, resultat, harRettNåEllerIFramtiden)` | personens Kelvin-sak i behandlingsflyt-faken (`null` = ingen sak) |
| `aktivSakIArena` | fss-proxy melder en aktiv Arena-sak |
| `harHistorikkIArena` | arenaoppslag melder (signifikant) historikk → `ArenaHistorikkRegel` false → Arena |
| `arenaSak = TestArenaSak(...)` | siste Arena-sak og vedtak for kant-til-kant (`ArenaService.harOppbruktRettighet`, via `ArenaoppslagFake`) |
| `skjermet` | NOM sier egen ansatt |

`TestJournalposter.leggTil { }` (`fakes/FakeKonstanter.kt`) returnerer en `TestJournalPost` registrert for SAF-, Gosys- og REST-fakene:

| Felt / hjelper | Standard / effekt |
| --- | --- |
| `person` eller `fnr` | ellers opprettes en ny `TestPerson`. `fnr` registrerer også en person |
| `brevkode` | `LEGEERKLÆRING` |
| `kanal` | `NAV_NO`. `SKAN_*` gjør standarddokumentet til en PDF (papir) |
| `tema`, `status` | `"AAP"`, `MOTTATT` |
| `fagsak = JournalpostSak(...)` | Joark-sak (for eksempel allerede journalført på et annet fagsystem) |
| `journalførendeEnhet`, `harEksisterendeGosysOppgave`, `avsenderMottaker` (standard ingen), `dokumenter` | som navnet sier |
| `digitalSøknad()` | brevkode SØKNAD, originaldokument i JSON, innhold `SøknadV0` |
| `papirsøknad()` | kanal `SKAN_NETS`, SØKNAD, PDF-dokumenter (søknad + vedlegg) |
| `legeerklæring()` | kanal `SKAN_NETS`, men en JSON-originalvariant, så den regnes som digital |
| `medUtenlandskOrgnr(orgnr)` | brukertype ORGNR, ingen avsender |
| `journalpostId` | tilfeldig. Sett den for å overskrive en eksisterende fake-journalpost (for eksempel for å endre tema midt i en test) |

Snarveier: `TestJournalposter.digitalSøknad()`, `.papirsøknad()`, `.legeerklæring()`. `TestJournalPost.tilJournalpost()` konverterer til domenets `Journalpost` for enhetstester. Merk at mottatt-datoen er *i dag*, i motsetning til SAFs faste 2020-12-01. `hoveddokument(brevkode, tittel)` og `vedlegg(tittel)` bygger eksplisitte dokumenter.

## Fakes og hjelpere i koden

| Fake | Merknader |
| --- | --- |
| `FakeUnleash` (object) | `AutomatiskKlageJournalforing` på, `KantTilKantV1` av, `PostmottakVelgTema` av, `DummyFeature` kaster exception. Overstyr én toggle med `object X : UnleashGateway by FakeUnleash { override fun isEnabled(...) }` |
| `fakeGatewayProvider(unleash)` | registrerer `FakeArenaoppslagGateway`, `FakeJournalpostGateway` og en Unleash-fake. `FakeJournalpostGateway.hentJournalpost` er `TODO()` |
| `FakeArenaoppslagGateway` | historikk bare for ident `09876543210`. Faste data for maksdato |
| `FakeStatistikkGateway` | registrerer hendelser, men `konstruer()` lager en ny instans hver gang, så du kan ikke lese dem tilbake |
| `InMemoryAvklaringsbehovRepository`, `InMemoryUnder18Repository` (objects) | repositorier i minnet med global tilstand. Ikke kall `clearMemory()`. Bruk unike id-er per test |
| `MockConnection().toDBConnection()` | en `DBConnection` der JDBC-kall kaster exception. Bare til å konstruere repositorier du ikke vil kalle |
| `FødselsnummerGenerator`, `AzureTokenGen` | gyldig fnr for en fødselsdato. Signerte testtokens |
| Testlokale: `TemaFlytUnleash` (`Flyttest`), `FakeUnleashGateway` (`ArenaOppgaveFlytTest`), `JoarkTemavalgUnleash` (`JoarkKafkaHandlerTest`), `BehandlingsflytGatewayMock` (`AvklaringsbehovsLøserTest`, alt `TODO()`) | eksempler på overstyringer per test |

## Legge til en fake

- HTTP: legg ruten til den tilhørende `Application.xFake()` (eller en ny server pluss systemegenskap i `FakeServers.setProperties`). La svarene styres av felt på `TestJournalPost`/`TestPerson` i stedet for globale brytere, slik at testene forblir uavhengige.
- Grensesnittnivå: implementer gatewayen med `companion object : Factory<X> { override fun konstruer() = instance }` (behold én instans hvis testene må inspisere den) eller som et Kotlin-`object`, og registrer den med `register<X>()`.
- Legg fakes som trengs av mer enn én modul i `lib-test`.
