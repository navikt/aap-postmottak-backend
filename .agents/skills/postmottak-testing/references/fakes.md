# Fakes and test data catalog

Everything here lives in `lib-test/src/main/kotlin/no/nav/aap/postmottak/test/` unless noted. Fake state is global JVM state that is never cleared: create fresh data per test through the builders.

## Fake HTTP servers (`FakeServers.kt`, started by `@Fakes`)

`@Fakes` (`FakesExtension`) starts one Netty server per system on a random port and sets the matching `INTEGRASJON_*` system properties, `NAIS_CLUSTER_NAME=LOCAL` and token endpoints. Use them through the real clients: `defaultGatewayProvider { register<FakeUnleash>() }`.

| System | Behaviour | Steer it with |
| --- | --- | --- |
| Texas (tokens) | `/token` issues an Azure-style JWT (azp `postmottak`, or `INTEGRASJON_POSTMOTTAK_AZP`); `/token/exchange` issues an OBO token keeping `NAVident`; `/introspect` says active | nothing |
| SAF GraphQL (`fakes/Saf.kt`) | `journalpost(id)` built from `TestJournalposter`; an unknown id gets a default `TestJournalPost` (digital legeerklæring). `DATO_REGISTRERT` is always 2020-12-01T10:00. `saker` returns an empty list. | `TestJournalposter.leggTil { }` |
| SAF REST | `hentdokument`: the `digitalSøknad` JSON for JSON variants, otherwise `sample.pdf` | `digitalSøknad = SøknadV0(...)` on the builder |
| dokarkiv (Joark) | PUT journalpost (accepts an oppdater request, or an endre-tema request with only `tema`), PATCH ferdigstill. Nothing is recorded. | nothing |
| aap-behandlingsflyt (`fakes/BehandlingsflytFake.kt`) | `finnEllerOpprett`: the person's first Kelvin sak, else a random new one. `ekstern/finn`: the registered person's `kelvinsaker` (empty if none); an **unregistered ident gets one random sak**. `hendelse/send`: 204. `finnBehandlingerAvType`: two klagebehandlinger | `TestPersoner.leggTil { kelvinSak = TestKelvinSak(...) }` |
| aap-oppgave, aap-statistikk | accept everything | nothing |
| tilgang | always grants access | nothing |
| Gosys oppgave API (`fakes/GosysoppgaveFake.kt`) | GET returns one oppgave only if the journalpost has `harEksisterendeGosysOppgave`; POST throws `ShouldNotBeCalledException` in that case; PATCH ok | `harEksisterendeGosysOppgave = true` |
| aap-arenaoppslag (`fakes/ArenaoppslagFake.kt`) | historikk and signifikant historikk = `TestPerson.harHistorikkIArena`; maksdato and siste utbetaling from `TestPerson.arenaSak` (404 if none) | `harHistorikkIArena`, `arenaSak = TestArenaSak(...)` |
| aap-fss-proxy (Arena WS) | `nyesteaktivesak`: `"1234"` if `harAktivSakIArena`, else 204 (also drives `harAktivSak`); `opprettoppgave`: appends the request to `FssOppgaver.oppgaver[ident]` and returns sak `SAK-5678`; kjøreliste: sak `"1234"` | `aktivSakIArena`; assert on `FssOppgaver.oppgaver` |
| PDL | identer: the requested ident (+ an AKTORID that the client filters out); fødselsdato is **always 1990-01-01**; geografisk tilknytning kommune 3207, ugradert | nothing |
| NOM (skjermet) | `TestPerson.erSkjermet`; **404 for unknown persons**, which makes the client throw | `skjermet = true` |
| NORG2 | arbeidsfordeling always enhet `"superNav!"`; active enheter only `"9999"`, so every journalførende enhet counts as inactive | nothing |
| veilarbarena | oppfølgingsenhet `"9999"` | nothing |
| EREG | empty response | nothing |
| Unleash server | **every** `PostmottakFeature` enabled (only used if `FakeUnleash` is not registered) | register `FakeUnleash` or your own |

## Test data builders

`TestPersoner.leggTil { }` (`modell/TestPerson.kt`) returns a `TestPerson` aged 19 with a generated valid fnr:

| Field | Effect |
| --- | --- |
| `kelvinSak = TestKelvinSak(saksnummer, periode, resultat, harRettNåEllerIFramtiden)` | the person's Kelvin sak in the behandlingsflyt fake (`null` = no sak) |
| `aktivSakIArena` | fss-proxy reports an active Arena sak |
| `harHistorikkIArena` | arenaoppslag reports (significant) history → `ArenaHistorikkRegel` false → Arena |
| `arenaSak = TestArenaSak(...)` | maksdato data for 11-12 fordeling (`Vurdering11_12Service`, uses `maxdatoOrdinaer` first) |
| `skjermet` | NOM says egen ansatt |

`TestJournalposter.leggTil { }` (`fakes/FakeKonstanter.kt`) returns a `TestJournalPost` registered for the SAF, Gosys and REST fakes:

| Field / helper | Default / effect |
| --- | --- |
| `person` or `fnr` | otherwise a new `TestPerson` is created; `fnr` also registers a person |
| `brevkode` | `LEGEERKLÆRING` |
| `kanal` | `NAV_NO`; `SKAN_*` makes the default dokument a PDF (papir) |
| `tema`, `status` | `"AAP"`, `MOTTATT` |
| `fagsak = JournalpostSak(...)` | Joark sak (e.g. already journalført on another fagsystem) |
| `journalførendeEnhet`, `harEksisterendeGosysOppgave`, `avsenderMottaker` (default none), `dokumenter` | as named |
| `digitalSøknad()` | brevkode SØKNAD, JSON original dokument, `SøknadV0` content |
| `papirsøknad()` | kanal `SKAN_NETS`, SØKNAD, PDF dokumenter (søknad + vedlegg) |
| `legeerklæring()` | kanal `SKAN_NETS` but a JSON original variant, so it counts as digital |
| `medUtenlandskOrgnr(orgnr)` | bruker type ORGNR, no avsender |
| `journalpostId` | random; set it to overwrite an existing fake journalpost (e.g. to change tema mid-test) |

Shortcuts: `TestJournalposter.digitalSøknad()`, `.papirsøknad()`, `.legeerklæring()`. `TestJournalPost.tilJournalpost()` converts to the domain `Journalpost` for unit tests; note its mottatt dato is *today*, unlike SAF's fixed 2020-12-01. `hoveddokument(brevkode, tittel)` and `vedlegg(tittel)` build explicit dokumenter.

## In-code fakes and helpers

| Fake | Notes |
| --- | --- |
| `FakeUnleash` (object) | `PostmottakVurder11_12_gosys` on, `AutomatiskKlageJournalforing` on, `PostmottakVelgTema` off, `DummyFeature` throws. Override one toggle with `object X : UnleashGateway by FakeUnleash { override fun isEnabled(...) }` |
| `fakeGatewayProvider(unleash)` | registers `FakeArenaoppslagGateway`, `FakeJournalpostGateway` and an Unleash fake. `FakeJournalpostGateway.hentJournalpost` is `TODO()` |
| `FakeArenaoppslagGateway` | history only for ident `09876543210`; fixed maksdato data |
| `FakeStatistikkGateway` | records hendelser, but `konstruer()` makes a new instance each time, so you cannot read them back |
| `InMemoryAvklaringsbehovRepository` (object) | in-memory `AvklaringsbehovRepository`; call `clearMemory()` between tests |
| `MockConnection().toDBConnection()` | a `DBConnection` whose JDBC calls throw; only for constructing repositories you will not call |
| `FødselsnummerGenerator`, `AzureTokenGen` | valid fnr for a birth date; signed test tokens |
| Test-local: `TemaFlytUnleash` (`Flyttest`), `FakeUnleashGateway` (`ArenaOppgaveFlytTest`), `JoarkTemavalgUnleash` (`JoarkKafkaHandlerTest`), `BehandlingsflytGatewayMock` (`AvklaringsbehovsLøserTest`, all `TODO()`) | examples of per-test overrides |

## Adding a fake

- HTTP: add the route to the matching `Application.xFake()` (or a new server plus system property in `FakeServers.setProperties`). Drive answers from fields on `TestJournalPost`/`TestPerson` instead of global switches, so tests stay independent.
- Interface level: implement the gateway with `companion object : Factory<X> { override fun konstruer() = instance }` (keep one instance if tests must inspect it) or as a Kotlin `object`, and register it with `register<X>()`.
- Put fakes needed by more than one module in `lib-test`.
