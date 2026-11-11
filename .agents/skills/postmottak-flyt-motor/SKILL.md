---
name: postmottak-flyt-motor
description: How the engine of aap-postmottak-backend really works - the job "motor" (Motor, JobbInput, ProsesserBehandlingJobbUtfører, retries, savepoints) and the flyt state machine (FlytOrkestrator, StegOrkestrator, BehandlingFlyt, StegStatus, avklaringsbehov, ventepunkter, tilbakeføring). Load before changing or debugging steps, flyter, avklaringsbehov, løsere, jobs or informasjonskrav, or when a behandling ends up in an unexpected steg, status or vent.
---

# Postmottak: flyt engine and motor

If the user or the code contradicts this skill, warn the user and ask whether the skill and/or `AGENTS.md` should be updated. See `AGENTS.md` → "When the user contradicts this documentation".

Two different engines that are easy to mix up:

| Term | What it is | Where |
| --- | --- | --- |
| **motor** | Generic job engine from aap-komponenter (`no.nav.aap.motor`). Polls the `JOBB` table, runs one `JobbUtfører` per DB transaction, retries, runs cron jobs. | Library. Postmottak jobs are registered in `flyt/.../prosessering/ProsesseringsJobber.kt`. |
| **flyt** (flytmotor) | Postmottak's state machine that moves one `Behandling` through the steps of its flyt. | `flyt/.../postmottak/flyt/` (`FlytOrkestrator`, `BehandlingFlyt`), `flyt/.../flyt/steg/` (`StegOrkestrator`). |

The flyt engine executes steps **only** inside the motor job `ProsesserBehandlingJobbUtfører` (type `flyt.prosesserBehandling`). API calls never run steps: they record a decision and enqueue that job.

## Mental model

```
anything (Kafka, API, another step, cron)
  └─ FlytJobbRepository.leggTil(JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId, behandlingId))
motor picks the job → new DB transaction → ProsesserBehandlingJobbUtfører.utfør
  ├─ TaSkriveLåsRepository.lås(behandlingId)          SELECT versjon ... FOR UPDATE
  ├─ FlytOrkestrator.forberedOgProsesserBehandling
  │    forberedBehandling   expire ventepunkter → rewind to earliest open behov → refresh informasjonskrav
  │                         of all EARLIER steps → rewind if the refresh says so
  │    prosesserBehandling  for steg in flyt starting at behandling.aktivtSteg():
  │                           StegOrkestrator.utfør(steg): START → OPPDATER_FAKTAGRUNNLAG → UTFØRER → AVKLARINGSPUNKT → AVSLUTTER
  │                           AVKLARINGSPUNKT finds an open avklaringsbehov for this steg → stop, emit hendelse, return
  │                         after the last steg: status AVSLUTTET, emit hendelse
  └─ verifiserSkrivelås                               versjon = versjon + 1
commit. On exception: commit everything up to the last savepoint, mark the job failed, retry later.
```

## The three flyter

| TypeBehandling | Steps (`StegType`) | Informasjonskrav (refreshed in OPPDATER_FAKTAGRUNNLAG) |
| --- | --- | --- |
| `Fordeling` | START_BEHANDLING → AVKLAR_FORDELING → AVKLAR_FORDELING_VIDERESEND → IVERKSETTES | AVKLAR_FORDELING: `JournalpostInformasjonskrav` |
| `Journalføring` | START_BEHANDLING → AVKLAR_TEMA → AVKLAR_SAK → SETT_FAGSAK → ENDELIG_JOURNALFØRING → VIDERESEND → IVERKSETTES | AVKLAR_TEMA: `JournalpostInformasjonskrav`, `SaksnummerInfoKrav` |
| `DokumentHåndtering` | START_BEHANDLING → DIGITALISER_DOKUMENT → OVERLEVER_TIL_FAGSYSTEM → IVERKSETTES | none |

Defined in `flyt/.../forretningsflyt/behandlingstyper/` and mapped in `flyt/.../flyt/BehandlingType.kt` (`utledType`). `IVERKSETTES` is `AvsluttBehandlingSteg` (a no-op). The business intent of each flyt is in the `postmottak-dokumentflyt` skill.

## Rules that are not obvious from reading one file

1. **A step never stops the flyt itself.** `utfør()` returns `Fullført` or `FantAvklaringsbehov`. The engine registers the behov in UTFØRER and stops in the next phase, AVKLARINGSPUNKT, if an *open* behov belongs to this steg. `FunnetAvklaringsbehov.kanFortsette()` is `true`; only `Stopp` (from AVKLARINGSPUNKT) is `false`.
2. **`utfør()` runs again from START every time the job resumes.** A steg that raised a behov is executed at least twice: once to discover the need, once after the løser stored the answer. Every step must start with "is there already a vurdering? then `Fullført`". This is also why steps must tolerate re-execution after rewinds and retries.
3. **Løsere store the answer, steps act on it.** `AvklaringsbehovLøsning.løs()` → `*Løser` writes a vurdering in the API transaction. The step reads it on the next run. Side effects towards other systems belong in steps, with a few exceptions (`AvklarSakLøser` calls behandlingsflyt `finnEllerOpprettSak`, `AvklarTemaLøser` sets or removes Gosys-vent).
4. **Resuming a Journalføring behandling past AVKLAR_TEMA rewinds it to AVKLAR_TEMA, in practice every time.** `InformasjonskravGrunnlagImpl` returns the krav that were *not* changed (`filterNot { ENDRET }`), contrary to the KDoc on `InformasjonskravGrunnlag`, and `SaksnummerInfoKrav` always returns `IKKE_ENDRET`. Verified log line: `Tilbakeført etter oppdatering av registeropplysninger fra 'AVKLAR_SAK' til 'AVKLAR_TEMA'`; in steg_historikk it reads `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX` (compact query in `postmottak-feilsoking/references/sql.md`). Do not "fix" this casually: it changes when steps re-run in production.
5. **Rewinding (tilbakeføring) runs no step code.** It only logs `TILBAKEFØRT` rows for each step on the way back and moves the cursor. Vurderinger are kept. It also emits a `DokumentflytStoppetHendelse` even though processing continues right after.
6. **Each step is constructed five times per pass** (`stegKonstruktør.konstruer` once per phase). `konstruer()` and constructors must be cheap and side-effect free.
7. **Savepoints make failures partially durable.** `StegOrkestrator` sets a savepoint after START and after OPPDATER_FAKTAGRUNNLAG. When a later phase throws, the transaction rolls back to the savepoint and then commits (`DBConnection.rollback` + `autoCommit = true` in komponenter). Earlier steps and the faktagrunnlag refresh of the failing step are persisted, the job retries from the failing step, and it is marked `FEILET` after 3 attempts. Verified.
8. **External calls inside steps can be repeated** (retries, rewinds, resumes). Use idempotent operations: `...HvisIkkeEksisterer` for Gosys, `finnEllerOpprettSak`, receivers that deduplicate on journalpost.
9. **Ventepunkter are only closed in these ways:** frist expired (`forberedBehandling` or cron `GjenopptaBehandlingJobbUtfører` at 07:00), the saksbehandler solves `MANUELT_SATT_PÅ_VENT`, `AvklaringsbehovOrkestrator.taAvVentPgaGosys`, or a step returning `Fullført` while the behandling has *no* non-vente avklaringsbehov at all (in any status). A step finishing does **not** close vent if, for example, `AVKLAR_TEMA` exists. Verified: `VENT_PA_GOSYS` stays open after the journalpost tema changes away from AAP.
10. **`Avklaringsbehovene.avbrytForSteg` ignores status.** `DigitaliserDokumentSteg` calls it when a vurdering exists, so a `DIGITALISER_DOKUMENT` solved by a saksbehandler ends as `AVBRUTT` (history `OPPRETTET, AVSLUTTET, AVBRUTT`). Verified. Use `hvemSomLøste()` (last `AVSLUTTET`) to find who solved it.
11. **`Status.erAvsluttet()` is true for both `IVERKSETTES` and `AVSLUTTET`.** A behandling is `IVERKSETTES` while in VIDERESEND, AVKLAR_FORDELING_VIDERESEND or IVERKSETTES, and `AVSLUTTET` only after the last step. `hentÅpenJournalføringsbehandling` only matches `OPPRETTET`/`UTREDES`.
12. **The motor "sakId" is the journalpostId.** `forBehandling(journalpostId, behandlingId)`. Jobs run strictly in order per (sakId, behandlingId, type). A `FEILET` job blocks later jobs of the same type in that group, and blocks saksbehandler actions on the behandling (`BehandlingUnderProsesseringException`).
13. **Data scope:** journalpost data is stored per journalpostId and shared by all behandlinger of that journalpost. Vurderinger are per behandling and copied Journalføring → DokumentHåndtering by `GrunnlagKopierer` (only repositories that override `kopier`). Fordeling → Journalføring copies nothing. Dokumentflyt has no informasjonskrav and fails if no journalpost has been stored by an earlier behandling. Verified.

## Where things are

| Concern | File (under `flyt/src/main/kotlin/no/nav/aap/postmottak/`) |
| --- | --- |
| Job entry point | `prosessering/ProsesserBehandlingJobbUtfører.kt` |
| Flyt loop, rewinds | `flyt/FlytOrkestrator.kt`, `flyt/BehandlingFlyt.kt` |
| Step phases, savepoints | `flyt/steg/StegOrkestrator.kt`, `journalpostogbehandling/flyt/StegStatus.kt` |
| Step results | `flyt/steg/{Fullført,FantAvklaringsbehov,Transisjon}.kt` |
| Avklaringsbehov model | `avklaringsbehov/{Avklaringsbehovene,Avklaringsbehov,AvklaringsbehovService}.kt`, `kontrakt/.../avklaringsbehov/Definisjon.kt` |
| Solving behov (API side) | `avklaringsbehov/AvklaringsbehovOrkestrator.kt`, `avklaringsbehov/løsning/*`, `avklaringsbehov/løser/*` |
| Behandling, status, steg history | `journalpostogbehandling/behandling/{Behandling,StegTilstand}.kt`, `repository/.../BehandlingRepositoryImpl.kt` |
| Job registry | `prosessering/ProsesseringsJobber.kt` |
| Outgoing hendelse to oppgave/statistikk | `hendelse/avløp/BehandlingHendelseServiceImpl.kt` → `prosessering/StoppetHendelseJobbUtfører.kt` |

## Before you change engine code

- Read the reference for the area first; most "bugs" here are intended or load-bearing.
- `BehandlingFlyt` is synced from aap-behandlingsflyt (KDoc says so). Keep diffs small to ease future syncs.
- Run the targeted unit tests and **one or two** relevant `Flyttest` methods, not the whole suite (see `postmottak-testing`).
- Use `../postmottak-testing/scripts/flyt-trace.sh` to read what the engine did instead of reading raw logs.

## References (read when relevant)

- `references/engine-walkthrough.md`: annotated algorithm for FlytOrkestrator, StegOrkestrator and BehandlingFlyt, all job triggers, status and steg_historikk, an annotated real trace.
- `references/avklaringsbehov.md`: all definisjoner, lifecycle, the two ways steps create behov, the løsning flow end to end, ventepunkter, validations.
- `references/motor-jobs-transactions.md`: motor semantics, exclusivity groups, retries and re-runs, savepoints, locks, cron jobs, idempotency.
- `references/recipes.md`: how to add a step, avklaringsbehov, informasjonskrav, vurdering repository, flyt, job or gateway, and what to watch for with in-flight behandlinger.
