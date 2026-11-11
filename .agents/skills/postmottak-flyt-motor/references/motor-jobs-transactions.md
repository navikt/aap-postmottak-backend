# Motor, jobs and transactions

The motor is the job engine from aap-komponenter (`no.nav.aap.motor`, sources in the Gradle cache as `motor-<version>-sources.jar`). This file describes how postmottak uses it. **Verified** means confirmed by a test run.

## Setup

- `app/.../App.kt` `lagMotor`: `Motor(dataSource, antallKammer = 4, jobber = ProsesseringsJobber.alle(), repositoryRegistry, gatewayProvider, logInfoProvider = PostmottakLogInfoProvider)`. Tests use 2 workers.
- Workers run on virtual threads. Each loops: pick one job in a "plukk" transaction, run it in a **new** transaction, then mark it `FERDIG` or record the failure in the plukk transaction. Idle workers sleep 500 ms.
- `RetryService(connection).enable()` at startup schedules all cron jobs, including the motor's own `oppgave.retryFeilede`.
- `PostmottakLogInfoProvider` adds `behandlingReferanse` and `journalpostId` to the MDC of every job with a behandlingId.

## JobbInput conventions in postmottak

| Builder | Meaning here |
| --- | --- |
| `forBehandling(journalpostId.referanse, behandlingId.id)` | Jobs about a behandling. The motor's "sakId" **is the journalpostId**. |
| `forSak(journalpostId.referanse)` | Jobs about a journalpost without a behandling (`VurderRelevantDokumentForAAPJobbUtfører`, all Arena jobs). |
| neither | Standalone jobs (cron jobs). |
| `medParameter` / `medPayload` | Properties or a JSON string. Helpers: `medJournalpostId`/`getJournalpostId` (`prosessering/JobbInputExtensions.kt`), `medArenaVideresenderKontekst` / `medAutomatiskJournalføringKontekst` (also set `forSak`). |
| `medCallId()` | Copies `callId` from the MDC so logs can be correlated. |

The job `type` string is persisted in `JOBB.type` and parsed back through the registry. **Never rename a type**, and keep a job registered in `ProsesseringsJobber.alle()` while rows of that type can exist. Removed features keep empty stubs for this reason (`RedigitaliseringKopierJobbUtfører`, `RedigitaliseringBehandlingJobbUtfører`).

## Ordering and parallelism

- Jobs with a sakId or behandlingId form an exclusivity group per **(sak_id, behandling_id, type)**. Only the oldest `KLAR`/`FEILET` job (by `neste_kjoring`) of a group can be picked.
- A `FEILET` job at the head of a group **blocks** that group until someone re-runs or cancels it. For `flyt.prosesserBehandling` it also blocks saksbehandler actions (`BehandlingUnderProsesseringException` checks for `KLAR` and `FEILET` jobs).
- Different types for the same behandling, and different behandlinger of the same journalpost, are different groups and can run in parallel. The behandling row lock (`TaSkriveLåsRepository`) serialises processing against API mutations.
- Jobs enqueued inside a job become visible at commit, so they run after the current job finishes (**verified** in traces: `flyt.hendelse` is picked after the `flyt.prosesserBehandling` that created it).
- The picker postmottak uses ignores `Prioritet`: ready jobs run in `neste_kjoring` order.
- komponenter also ships a second picker (`plukkJobbV2`, column `JOBB.kjorbar`, `skjedulerJobber`) that honours `prioritet`, switched on with `Motor(enableV2 = ...)`. Postmottak does not switch it on, so `kjorbar` stays `false` and the rules above apply.
- Motor cron `jobber.arkiverFerdigstilte` (daily 01:00) moves `FERDIG` jobs whose `neste_kjoring` is more than 30 days old to `JOBB_ARKIV`/`JOBB_HISTORIKK_ARKIV`.

## Failure, retries and re-runs

- On an exception the job's transaction is rolled back to its last savepoint (see below) and the rest committed, a `FEILET` row with the stack trace is added to `JOBB_HISTORIKK`, and the job stays `KLAR` until it has failed `retries` times; then it becomes `FEILET`.
- `retries` is the **total number of attempts** (default 3). `antallRetriesForsøkt()` is the number of earlier failed attempts (0 on the first). No backoff is configured, so a retry happens on the next pick.
- Motor cron `oppgave.retryFeilede` (07:00, 12:00, 15:00, 20:00) sets **all** `FEILET` jobs back to `KLAR` without resetting history. The next failure marks them `FEILET` again immediately, and `antallRetriesForsøkt()` keeps growing. Several Arena jobs rely on this to reach their fallback.

| Job (class) | `type` | retries | Fallback / note |
| --- | --- | --- | --- |
| `ProsesserBehandlingJobbUtfører` | `flyt.prosesserBehandling` | 3 | none |
| `StoppetHendelseJobbUtfører` | `flyt.hendelse` | 3 | none |
| `VurderRelevantDokumentForAAPJobbUtfører` | `vurderdokument.innkommende` | 3 | none |
| `GjenopptaBehandlingJobbUtfører` | `batch.gjenopptaBehandlinger` | 3 | cron daily 07:00 |
| `JoarkAvstemmerJobbUtfører` | `Joark-avstemming` | 3 | cron daily 03:00 (`navn` and `type` look swapped) |
| `SendSøknadTilArenaJobbUtfører` | `arena.søknad` | 4 | `antallRetriesForsøkt ≥ 3` → ManuellJournalføring job |
| `OppprettOppgaveIArenaJobbUtfører` | `arena.oppgaveoppretter` | 4 | no enhet or `≥ 2` → ManuellJournalføring job |
| `JournalførSøknadForVurdering11_12JobbUtfører` | `arena.søknad.vurder1112` | 4 | no aktiv Arena sak or `≥ 3` → ManuellJournalføring job |
| `SendTilArenaKjørelisteBehandling` | `arena.kjøreliste` | 4 | `≥ 2` → ManuellJournalføring job |
| `AutomatiskJournalføringJobbUtfører` | `arena.automatiskjournalføring` | 3 | `≥ 3` → ManuellJournalføring job; only reachable after `FEILET` + retryFeilede |
| `ManuellJournalføringJobbUtfører` | `arena.manuell.journalføring` | 6 | enhet and `< 3` → Gosys JFR oppgave, otherwise FDR (fordeling) oppgave |
| `Redigitalisering*JobbUtfører` | `redigitalisering.kopier`, `redigitalisering.behandling` | 3 | no-op stubs |

Operations endpoints from `motorApi` (prod requires the Team AAP role): `GET /drift/api/jobb/feilende`, `GET /drift/api/jobb/planlagte-jobber`, `GET /drift/api/jobb/sisteKjørte`, `GET /drift/api/jobb/rekjor/{jobbId}`, `GET /drift/api/jobb/avbryt/{jobbId}`, `GET /drift/api/jobb/rekjorAlleFeilede`, `POST /drift/api/jobb/{jobbId}/kjor`.

## Savepoints: failures are partially durable

- `StegOrkestrator.utfør` calls `behandlingRepository.markerSavepoint()` after the START phase and after the OPPDATER_FAKTAGRUNNLAG phase of every steg. `DBConnection` keeps only the latest savepoint.
- On an exception, komponenter's `DBTransaction` calls `DBConnection.rollback()`, which rolls back **to the savepoint** if one exists, and then in `finally` sets `autoCommit = true`, which makes JDBC **commit** the remaining transaction.
- Result: everything written before the last savepoint is committed: progress through earlier steps, their vurderinger and steg_historikk, jobs they enqueued (new behandlinger, hendelser), and the faktagrunnlag refresh of the failing step. Only work after the savepoint is lost. The retry resumes at the failing steg.
- **Verified:** a DokumentHåndtering behandling whose DIGITALISER_DOKUMENT step threw kept START_BEHANDLING as completed, had `DIGITALISER_DOKUMENT:START/OPPDATER_FAKTAGRUNNLAG` rows from each attempt, and its job became `FEILET` after 3 attempts.
- Calls to other systems made before the exception are not rolled back. Combined with retries this means steps can repeat external calls.
- API requests and the Kafka handler set no savepoints: they are all-or-nothing.

## Locks and versjon

`TaSkriveLåsRepository.lås` = `SELECT versjon ... FOR UPDATE`; `verifiserSkrivelås` = `UPDATE ... SET versjon = versjon + 1 WHERE versjon = old` (requires exactly one row). Taken by `ProsesserBehandlingJobbUtfører`, `løs-behov`, `sett-på-vent` and `GET /forbered` (which bumps versjon even when it enqueues nothing). The frontend sends `behandlingVersjon` with mutations; a stale value gives `OutdatedBehandlingException`.

## Idempotency checklist for steps and jobs

- Start `utfør()` by reading the existing vurdering; return `Fullført` if it is already decided.
- Prefer lookups and "create if missing" calls to other systems: `GosysOppgaveGateway.*HvisIkkeEksisterer`, `BehandlingsflytGateway.finnEllerOpprettSak`.
- Joark updates (`førJournalpostPåFagsak`, `ferdigstill...`) and `BehandlingsflytGateway.sendHendelse` may be repeated on retry; the receivers must tolerate that.
- Never assume one run per behandling: rewinds, resumes and retries re-run steps (section "informasjonskrav rewind" in `engine-walkthrough.md`).

## The Kafka stream is not the motor

`JoarkKafkaHandler` runs on Kafka Streams threads with exactly-once processing. Each record uses its own `datasource.transaction` blocks. An unhandled exception shuts the stream client down (`ProcessingExceptionHandler` → `SHUTDOWN_CLIENT`), `/actuator/live` returns 503 and Kubernetes restarts the pod; the record is processed again after restart. Keep handler logic thin: look things up, then enqueue jobs.

## Waiting for jobs in tests

`TestUtil(dataSource, cronTypes).ventPåSvar(sakId?, behandlingId?, maxTid = 20)` polls until no job matching the filter is in a state other than `FERDIG`/`FEILET`, ignoring the cron types you pass. It does **not** fail on timeout (it only logs `Avbryter venting nå`) and treats `FEILET` as finished. When a flyt test fails strangely, check for `FEILET` jobs and their `JOBB_HISTORIKK.feilmelding` before anything else.
