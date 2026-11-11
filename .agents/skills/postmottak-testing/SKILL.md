---
name: postmottak-testing
description: How to write, run and debug tests in aap-postmottak-backend - fakes over mocks (lib-test FakeServers, TestJournalposter, TestPersoner, FakeUnleash, TestDataSource), which test type to pick (pure, step/løser, repository, klient, Kafka topology, motor job, Flyttest), running narrow test sets without sandbox timeouts, and reading large flyt-test output cheaply with the bundled scripts. Load before writing, changing, running or debugging any test.
---

# Testing in postmottak

If the user or the code contradicts this skill, warn the user and ask whether the skill and/or `AGENTS.md` should be updated. See `AGENTS.md` → "When the user contradicts this documentation".

## Principles

1. **Prefer fakes over mocks.** A fake is a working implementation with controllable state:
   - External HTTP systems: annotate the class with `@Fakes` (starts local fake servers, see `references/fakes.md`) and use the real clients through `defaultGatewayProvider { register<FakeUnleash>() }`. Control their answers with `TestJournalposter` and `TestPersoner`.
   - Gateways at interface level: small in-code fakes registered with `createGatewayProvider { register<FakeX>() }` (`FakeUnleash`, `FakeArenaoppslagGateway`, `FakeStatistikkGateway`, ...). Registering an implementation replaces any earlier one of the same interface.
   - Database: real Postgres through `TestDataSource` (komponenter dbtest; each instance is a fresh database cloned from a migrated template) and `postgresRepositoryRegistry.provider(connection)`. `InMemoryAvklaringsbehovRepository` exists for pure engine logic.
2. **Mocks (mockk) are fine when a fake would not add value:** forcing failures that are hard to produce (e.g. `BadRequestHttpResponsException` from Gosys), proving that something was *not* called when there is no observable state, isolating an irrelevant collaborator, and wiring `JoarkKafkaHandler` into a `TopologyTestDriver`. Many existing step tests are mock-based; keep them working, but write new tests fake-first and assert stored state rather than call counts.
3. **Go through the real wiring.** Build steps with `MittSteg.konstruer(repositoryProvider, gatewayProvider)` and løsere with `MinLøser.konstruer(...)`, so missing registrations and wrong lookups fail in tests.
4. **Pick the cheapest test type that proves the behaviour** (table below). Add a `Flyttest` scenario only for behaviour that spans steps, jobs, behandlinger or Kafka.

## Test types

| Type | Proves | Infrastructure | Example |
| --- | --- | --- | --- |
| Pure unit | rules, parsing, domain logic | none | `RegelResultatTest`, `JournalpostTest`, `SaksinfoTest` |
| Step or løser, fake-first | a step's decisions with real DB and HTTP wiring | `TestDataSource` + `@Fakes` | `forretningsflyt/steg/journalføring/AvklarSakStegMedFakesTest` |
| Step, mock-based (existing style) | decision tables in isolation | mockk | `AvklarSakStegTest`, `AvklarFordelingStegTest` |
| Repository | SQL, grunnlag/aktiv, `kopier` | `TestDataSource` | `repository/src/test/...`, `SaksnummerRepositoryImplTest` |
| Klient | HTTP clients against fakes | `@Fakes` | `klienter/src/test/...` |
| Kafka topology | `JoarkKafkaHandler` filtering and branching | `TopologyTestDriver`, no broker | `JoarkKafkaHandlerTest` |
| Motor job chain | job sequences, fallbacks | `TestDataSource` + `Motor` + `@Fakes` | `fordeler/arena/ArenaOppgaveFlytTest` |
| Flyt, end to end in-process | journeys across behandlinger, avklaringsbehov, vent, Kafka | Postgres + Kafka containers + `Motor` + `@Fakes` | `flyt/Flyttest` |

Templates for each type: `references/test-templates.md`. What every fake returns: `references/fakes.md`.

## Running tests without timeouts

Prerequisites: Docker for Testcontainers (Postgres 18; Kafka `apache/kafka-native` for `Flyttest`). With Colima, export the three `TESTCONTAINERS_*`/`DOCKER_HOST` variables from the README. Gradle uses a JDK 25 toolchain.

- **Do not run the whole suite to "check".** `./gradlew test` or `build` runs all modules in parallel (`org.gradle.parallel=true`) and each module's tests in `cores / 2` JVM forks, each starting its own containers. In a sandbox this is slow and can time out.
- Use the bundled script; it forces `--rerun` (otherwise Gradle's up-to-date check and build cache can skip the task and you see zero tests), caps `--max-workers=2`, keeps the full log in a temp file and prints a short summary with failure messages:
  ```bash
  .agents/skills/postmottak-testing/scripts/run-tests.sh flyt 'no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarSakStegTest'
  .agents/skills/postmottak-testing/scripts/run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.kjører en manuell søknad igjennom hele flyten'
  .agents/skills/postmottak-testing/scripts/run-tests.sh repository '*JournalpostRepositoryImplTest'
  ```
  Filters: fully qualified class, class + method name (spaces allowed, no parameters for parameterized tests), or `*Suffix`.
- **Flyt tests: one method at a time while iterating**, the class when done. Measured on a laptop: a single `Flyttest` method ≈ 15 s wall clock, the whole class (24 tests) ≈ 40 s, a fake-first step test class ≈ 6–9 s. Never start several flyt-test classes or modules in parallel.
- Compile only: `./gradlew :flyt:compileTestKotlin`. Lint: `./gradlew detekt` (not part of `check`).
- Where tests live: engine, steps, løsere, jobs, Kafka in `flyt`; repository implementations in `repository` (a few in `flyt`); clients in `klienter`; app and API in `app`; `kontrakt` has one tiny test.
- `<module>/build/test-results/test/` only contains the classes of the **last** run.
- `app`'s `ApiTest` writes `openapi.json` in the repo root (gitignored) as a side effect.

## Reading output cheaply

Flyt-test reports are huge (`Flyttest` XML ≈ 1 MB, one stdout block per class). Do not open them. Instead:

```bash
.agents/skills/postmottak-testing/scripts/flyt-trace.sh flyt                 # engine trace from the last :flyt:test run
.agents/skills/postmottak-testing/scripts/flyt-trace.sh flyt '\[b=2\]'       # only behandlingId 2
```

It keeps job pick/finish/enqueue, steps, results, avklaringsbehov, rewinds, warnings, errors and exception lines (about 10× smaller). The engine semantics behind the lines are in the `postmottak-flyt-motor` skill.

## Pitfalls seen in this repo

1. `TestUtil.ventPåSvar` **never fails**: after `maxTid` it logs "Avbryter venting nå" and returns, and it treats `FEILET` jobs as finished. If a flyt test sees a half-processed behandling, look for `Feil under prosessering av jobb` in the trace before anything else.
2. Fake state is global and never cleared: `TestJournalposter`, `TestPersoner`, `FssOppgaver` are JVM singletons. Always create new data through the builders (random ids) and never assert on global counts.
3. Create journalposter through `TestJournalposter.leggTil { }`: it registers the person, and the NOM fake answers 404 for unknown persons. Builder defaults: digital **legeerklæring**, kanal NAV_NO, tema AAP, status MOTTATT, new person, no avsenderMottaker.
4. `defaultGatewayProvider()` **without** `register<FakeUnleash>()` uses the real Unleash client against the HTTP fake, which reports **every** toggle enabled. `FakeUnleash` instead has `PostmottakVelgTema` off and throws for `DummyFeature`. Toggle overrides in tests are shared objects; reset them in `finally` (see `TemaFlytUnleash` in `Flyttest`).
5. Tests run as "dev": `Aldersregel` and `SøknadRegel` are inactive, so fordeling differs from prod.
6. `PrometheusProvider.prometheus` is a global `lateinit`. `@Fakes` and `WithDependencies` set it; plain unit tests that construct clients or some steps must set it themselves.
7. Every `@Fakes` class starts a fresh set of about 16 fake servers and repoints the system properties. They are never stopped on purpose: clients are lazy singletons that keep the URL they first read.
8. AssertJ `anySatisfy { }` / `allSatisfy { }` take consumers. A lambda that only computes a Boolean asserts nothing (the last assertion in `Flyttest."Blir satt på vent for etterspørring av informasjon"` is like that). Use `assertThat` inside, or `anyMatch`.
9. `InMemoryAvklaringsbehovRepository` is a global object: call `clearMemory()` between tests.
10. Do not run a file write and a Gradle test in the same parallel batch of tool calls; Gradle can start before the file exists.
11. Dokumentflyt has no informasjonskrav: a `DokumentHåndtering` behandling created directly in a test fails in DIGITALISER_DOKUMENT unless a journalpost has been stored for the journalpostId first (run `JournalpostInformasjonskrav` or a Journalføring behandling).
12. Fake dates: SAF reports `DATO_REGISTRERT` 2020-12-01, which becomes the journalpost's mottatt dato. Arena maksdato test data is built relative to that date.
13. `FlytJobbRepository.hentJobberForSak/ForBehandling` parse job types through the motor's global `JobbType` registry, which is filled only when a `Motor` is constructed. In a class without a motor this fails with `Key <type> is missing in the map`, or passes when another class ran first in the same JVM. Query `JOBB` with SQL there (template 4).
14. `TopologyTestDriver` rejects an empty `bootstrap.servers`: give `StreamsConfig(brokers = "localhost:9092")` a dummy value.

## References

- `references/fakes.md`: every fake server and in-code fake, what it returns, and how to steer it.
- `references/test-templates.md`: copyable templates for each test type, including how to solve avklaringsbehov and send Kafka records in flyt tests.
- `scripts/run-tests.sh`, `scripts/flyt-trace.sh`: run narrow test sets; extract a compact engine trace.
