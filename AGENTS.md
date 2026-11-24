# AGENTS.md: aap-postmottak-backend

Postmottak receives incoming documents (journalposter in Joark) on tema AAP and gets each one to the right place: journalført and delivered to Kelvin (aap-behandlingsflyt), routed to Arena, or handed to humans as Gosys oppgaver. Owned by team AAP (Kelvin) in Nav. Kotlin on JDK 25, Ktor, Postgres with Flyway, Kafka Streams, and the job engine "motor" from aap-komponenter.

Code, domain terms, comments and commit messages are Norwegian. Keep domain identifiers as they are (`behandling`, `avklaringsbehov`, `steg`, `journalføring`, `vurdering`, ...).

## Skills: load the one that matches your task

| Skill | Load when |
| --- | --- |
| `postmottak-flyt-motor` | Changing or debugging steps, flyter, avklaringsbehov, løsere, informasjonskrav or jobs; a behandling is in an unexpected steg, status or vent. Covers `FlytOrkestrator`, `StegOrkestrator`, the motor, retries, savepoints. |
| `postmottak-dokumentflyt` | Business logic and routing: what happens to a document, why it went to Kelvin, Arena or Gosys, what is automatic per document type, feature toggles. |
| `postmottak-testing` | Writing, running or debugging any test. Has the scripts that keep test runs short and their output small. |
| `postmottak-feilsoking` | Support questions and incidents in dev or prod: drift and motor APIs, log fields, verified read-only SQL. |

Skills live in `.agents/skills/<name>/`. Each `SKILL.md` points to `references/` files; read those only when the task needs them.

## When the user contradicts this documentation

This file and the skills can be outdated or wrong. If the user says or asks for something that contradicts `AGENTS.md` or a skill (business rules, engine behaviour, conventions, commands, file locations, ...):

1. Warn the user. Name the contradiction and the file and section it comes from.
2. Do not silently pick one side. If the code can settle it, check the code and say what it shows.
3. Ask whether `AGENTS.md`, the skill (`SKILL.md` or a `references/` file), or both should be updated. Only edit the documentation after the user says yes.

The same applies when you find that the code contradicts the documentation.

## Vocabulary

| Term | Meaning |
| --- | --- |
| journalpost | A registered shipment in Joark with one or more dokumenter. Read through SAF, written through dokarkiv. |
| behandling | One run of a flyt for one journalpost (table `BEHANDLING`). A journalpost usually gets Fordeling → Journalføring → DokumentHåndtering. |
| flyt, steg | The ordered steps of a behandling type (`StegType`), executed by the flyt engine. |
| avklaringsbehov | Something a saksbehandler must decide (AVKLAR_TEMA, AVKLAR_SAK, DIGITALISER_DOKUMENT, ...) or a vent (MANUELT_SATT_PÅ_VENT, VENT_PA_GOSYS). Solved through a løsning and its løser. |
| vurdering, grunnlag | A stored decision per behandling (tema, sak, fordeling, digitalisering, overlevering); grunnlag rows point to the active one. |
| informasjonskrav, faktagrunnlag | Data a steg refreshes from other systems before it runs (journalpost, Kelvin saker). |
| motor, jobb | The job engine and its jobs (table `JOBB`). Steps only run inside the job `flyt.prosesserBehandling`. |
| Kelvin / Arena / Gosys | New AAP system (behandlingsflyt) / old AAP system / Nav's generic oppgave system. |

More terms: `postmottak-dokumentflyt` → Vocabulary.

## Modules

| Module | Contents |
| --- | --- |
| `kontrakt` | DTOs and enums shared with the frontend and other apps (`StegType`, `Definisjon`, `Status`, `TypeBehandling`). **Published as a library** when it changes on `main`; add `(MAJOR)` to the commit message for breaking changes. |
| `flyt` | The core: Kafka mottak (`mottak/`), fordeling rules and Arena (`no.nav.aap.fordeler`), flyt engine (`flyt/`), steps (`forretningsflyt/steg/<flyt>/`), avklaringsbehov, løsere, informasjonskrav, jobs (`prosessering/`), and the repository and gateway interfaces. |
| `repository` | Postgres implementations and Flyway migrations (`src/main/resources/flyway/`). |
| `klienter` | HTTP clients that implement the gateway interfaces (SAF, dokarkiv, PDL, behandlingsflyt, Gosys oppgave, Arena, NOM, Norg, Unleash, ...). |
| `api` | Ktor routes for the saksbehandler frontend, drift and audit log. |
| `app` | `App.kt`: wiring of database, motor, Kafka stream and routes. `app/src/test`: `TestApp` (local run with fakes) and OpenAPI generation. |
| `lib-test` | Fake servers and test data (`@Fakes`, `TestJournalposter`, `TestPersoner`, `FakeUnleash`), used by the other modules' tests. |

Dependencies point inwards: `repository`, `klienter` and `api` depend on `flyt`; `flyt` depends only on `kontrakt`; `app` wires everything.

## The journey in brief

```text
Joark Kafka event → JoarkKafkaHandler (filters; Kafka Streams thread, not the motor)
  → job vurderdokument.innkommende → INNKOMMENDE_JOURNALPOST (EVALUERT / IGNORERT / GOSYS_FDR)
  → Fordeling behandling: Kelvin or Arena (rules; toggle KantTilKantV1 can also route qualifying
      kant-til-kant søknader to Kelvin. See ArenaService for current criteria. Always automatic;
      AVKLAR_FORDELING is deprecated and only exists in historical data)
      Arena → Arena jobs or a Gosys oppgave
  → Journalføring behandling: AVKLAR_TEMA, AVKLAR_SAK, SETT_FAGSAK, ENDELIG_JOURNALFØRING, VIDERESEND
  → DokumentHåndtering behandling: DIGITALISER_DOKUMENT, OVERLEVER_TIL_FAGSYSTEM → hendelse to behandlingsflyt
```

API calls never run steps: they store a decision and enqueue `flyt.prosesserBehandling`. Details: `postmottak-dokumentflyt` (business) and `postmottak-flyt-motor` (engine).

## Build, test and run

- Prerequisites: JDK 25 toolchain and Docker for Testcontainers (Postgres, Kafka). With Colima, export the variables listed in `README.md`.
- Compile check (fast): `./gradlew :flyt:compileTestKotlin`, or `./gradlew assemble`.
- **Always run the full test suite with `./gradlew test` before saying a task is done.** Run it once at the end, not while iterating. It runs every module in parallel with `cores / 2` test JVMs, and each JVM starts its own containers. Expect several minutes, so give the command a long timeout or run it in the background. Tests in unchanged modules may come from Gradle's build cache, which is fine. Narrow test runs have missed failures in other test classes that construct the same steps or løsere.
- While iterating, run narrow test sets with the script. It caps workers and prints a compact summary. Trust Gradle's up-to-date check and build cache; do not force `--rerun`:
  ```bash
  .agents/skills/postmottak-testing/scripts/run-tests.sh flyt 'no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarSakStegTest'
  .agents/skills/postmottak-testing/scripts/run-tests.sh flyt 'no.nav.aap.postmottak.flyt.Flyttest.kjører en manuell søknad igjennom hele flyten'
  .agents/skills/postmottak-testing/scripts/run-tests.sh repository '*JournalpostRepositoryImplTest'
  ```
  A single `Flyttest` method takes about 15 s, the whole class about 40 s. Run one method at a time while iterating. Read flyt-test output with `.agents/skills/postmottak-testing/scripts/flyt-trace.sh flyt`, not the 1 MB XML report.
- Lint: `./gradlew detekt` (not part of `check`; CI runs it on pull requests).
- Run locally: `./gradlew :app:runTestApp` (port 8070; fake servers, Postgres in Testcontainers, seeded example behandlinger). Against dev: see `README.md`.
- `openapi.json` in the root is generated (`./gradlew :app:genererOpenApiJson`, also by `ApiTest`) and gitignored.

## Conventions

- Tests: avoid mocking. Prefer in-memory repositories and fakes (`@Fakes`, `TestJournalposter`); use `TestDataSource` where a real repository is needed. Use a mock only when the behavior cannot reasonably be tested with a fake. New step tests follow template 1 in `postmottak-testing/references/test-templates.md`.
- Wiring is by registries; new classes that are not registered fail at runtime, not at compile time:
  - repositories: interface in `flyt`, implementation with `companion object : Factory<...>` in `repository`, registered in `repository/.../PostgresRepositoryRegistry.kt`;
  - gateways: interface in `flyt`, implementation in `klienter`, registered in `klienter/.../DefaultGatewayProvider.kt`, plus a fake in `lib-test`;
  - jobs: registered in `flyt/.../prosessering/ProsesseringsJobber.kt`. Never rename a job `type`; rows with the old type must still parse;
  - avklaringsbehov løsninger: added to `utledSubtypes()` in `flyt/.../avklaringsbehov/løsning/AvklaringsbehovLøsning.kt`.
  Step-by-step recipes, including what happens to behandlinger already in flight: `postmottak-flyt-motor/references/recipes.md`.
- Each repository owns its own tables. Do not have multiple repositories read or write the same table; add new data to a table owned by the repository responsible for it.
- Flyway: add a new `V1.<next>__beskrivelse.sql` migration, using the next version after the latest migration in the repository; never edit an applied migration. **Every table needs a primary key**: all tables are published for Datastream, and Postgres rejects `UPDATE`/`DELETE` on a published table without one. Tests do not catch this. Prod runs Postgres 16, tests use 18, so avoid newer SQL features.
- Error handling, as in aap-behandlingsflyt: invalid input from a saksbehandler (in løsere, løsninger and API code) throws `UgyldigForespørselException` from komponenter, which becomes HTTP 400 with the message shown to the user. Do not use `require`/`check` for this: `IllegalArgumentException` becomes a 500 with "En ukjent feil oppsto". Keep `require`/`check`/`error` for internal invariants (developer errors).
- Personal data (fødselsnummer, names) only goes to the `team-logs` logger (`LoggerFactory.getLogger("team-logs")`), never to the normal log.
- Style: `.editorconfig` (4 spaces, 120 columns, no wildcard imports), detekt rules in `config/detekt/detekt.yml`. Match the surrounding code; most code has no comments.
- Do not commit or push unless the user explicitly asks for it.
- Commits: `AAP-<jira-number> <kort beskrivelse>` or `AAP-BAU <kort beskrivelse>` for maintenance, in Norwegian. A merge to `main` deploys to dev and prod.

## Load-bearing quirks: read before "fixing"

1. When a behandling resumes, every informasjonskrav of earlier steps is refreshed, and the behandling is rewound to the earliest steg whose informasjonskrav returned `ENDRET` (same semantics as aap-behandlingsflyt). An informasjonskrav that wrongly reports `ENDRET` causes a rewind on every resume. Before November 2026 the result was inverted, so Journalføring was rewound to AVKLAR_TEMA on almost every resume; old steg history shows this.
2. Every step must be idempotent: `utfør()` runs again after each stop, rewind and retry, and calls to other systems repeat.
3. A failing job commits everything up to the last savepoint (after START and OPPDATER_FAKTAGRUNNLAG of each steg), then retries. When its attempts are used up (3 for `flyt.prosesserBehandling`) it is `FEILET` and blocks later jobs of the same type for that behandling.
4. Vurderinger are per behandling; Journalføring copies them to DokumentHåndtering, Fordeling copies nothing. Journalpost data is shared per journalpostId.
5. Tests run with dev rules: `Aldersregel` and `SøknadRegel` are active only in prod.
6. `TestUtil.ventPåSvar` never fails on timeout and treats `FEILET` jobs as done. When a flyt test behaves oddly, look for failed jobs first.

Details and the reasoning behind each: `postmottak-flyt-motor` → "Rules that are not obvious from reading one file", and `postmottak-testing` → "Pitfalls seen in this repo".
