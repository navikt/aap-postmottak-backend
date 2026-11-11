---
name: postmottak-feilsoking
description: Investigate why a journalpost or behandling in aap-postmottak-backend is stuck, failed, on vent or ended up in the wrong place, in dev or prod. Covers the drift and motor APIs, log fields, an investigation playbook, and verified read-only SQL for behandlinger, steg history, avklaringsbehov, jobs, fordeling and vurderinger. Load for support questions, incidents and "where is document X" questions.
---

# Troubleshooting postmottak

If the user or the code contradicts this skill, warn the user and ask whether the skill and/or `AGENTS.md` should be updated. See `AGENTS.md` → "When the user contradicts this documentation".

Background on what *should* happen: `postmottak-dokumentflyt` (business flow) and `postmottak-flyt-motor` (engine semantics).

## Sources, cheapest first

1. **Drift API** (Azure AD; role `AAP_DRIFT_LES` for reading, DRIFTE for actions):
   - `GET /api/drift/journalpost/{journalpostId}/info`: innkommende status, brevkode, tema, regelresultat, Joark status, kanal, saksnummer, and every behandling with steg, status and avklaringsbehov history. Fødselsnummer is stripped from the response.
   - `POST /api/drift/person/journalposter/søk` with `{ "ident": "..." }`: innkommende journalposter for a person.
   - `POST /api/drift/behandling/{referanse}/prosesser`: enqueue processing. Safe to repeat (each call queues one more processing run, and steps are written to tolerate re-runs), but it queues behind a `FEILET` job for the same behandling.
   - `POST /api/drift/journalpost/{journalpostId}/kopier-journalpost`: copy a journalpost in Joark (tema AAP only).
2. **Motor API** (prod: Team AAP role): `GET /drift/api/jobb/feilende`, `GET /drift/api/jobb/planlagte-jobber`, `GET /drift/api/jobb/sisteKjørte`, `GET /drift/api/jobb/rekjor/{jobbId}`, `GET /drift/api/jobb/avbryt/{jobbId}`, `GET /drift/api/jobb/rekjorAlleFeilede`, `POST /drift/api/jobb/{jobbId}/kjor` (run a `KLAR` job now). `rekjor` and `avbryt` only act on `FEILET` jobs.
3. **Frontend flyt view**: `GET /api/behandling/{referanse}/flyt` returns `prosessering.status` (`JOBBER`, `FEILET`, `FERDIG`) and the error text of a failed job.
4. **Logs** (Loki/Elastic): MDC fields `journalpostId`, `behandlingReferanse`, `behandlingId`, `sakId` (= journalpostId), `jobbType`, `callId`. Job failures are logged as `Feil under prosessering av jobb :: [type] ... <ExceptionClass>. Se secure logs.`; the stack trace is only in team-logs (secure) and in `jobb_historikk.feilmelding` (SQL "Jobs"). The engine's own lines are listed in `postmottak-testing/scripts/flyt-trace.sh` (`Behandler steg`, `Stopper opp ved`, `Tilbakeført`, ...).
5. **SQL** (read-only): `references/sql.md`. Avoid selecting fødselsnummer unless needed, and never paste them into tickets or chats.

## Playbook: "where is journalpost X / why did it not reach Kelvin?"

| Observation | Meaning | Next step |
| --- | --- | --- |
| No innkommende journalpost and no behandling | The Kafka event was filtered (status not `MOTTATT`, kanal EESSI, tema not AAP), or the `vurderdokument.innkommende` job has not succeeded | SQL "Jobs" for the journalpost. Otherwise check Joark status, tema and kanal in SAF. Repeated pod restarts with stream errors mean the Kafka handler throws. `JoarkAvstemmer` creates a Gosys FDR oppgave for MOTTATT AAP journalposter without regelresultat after 5 days. |
| Innkommende `IGNORERT` (`ALLEREDE_JOURNALFØRT` / `UTGÅTT`) | By design | none |
| Innkommende `GOSYS_FDR` (`MANGLER_IDENT` / `ORGNR`) | By design: a Gosys fordelingsoppgave was created | Handled in Gosys |
| `EVALUERT`, Fordeling behandling `AVSLUTTET`, regelresultat ARENA | Routed to Arena | Check `arena.*` jobs (SQL "Jobs"). Fallbacks end in a Gosys JFR/FDR oppgave. |
| Fordeling vurdering `ARENA_VURDER_11_12` | Søknad with < 26 weeks left of ordinær AAP in Arena and no 11-12 vurdering yet: journalført on the Arena sak without Arena oppgave, Gosys oppgave for 11-12 (stub, not created yet). Log: "fordeles til ARENA_VURDER_11_12" | Check job `arena.søknad.vurder1112` and `arena.automatiskjournalføring` (SQL "Jobs") |
| Journalføring open with AVKLAR_TEMA or AVKLAR_SAK open | Waiting for a saksbehandler | none |
| Journalføring open with `VENT_PA_GOSYS` / `MANUELT_SATT_PÅ_VENT` open | On vent until frist (cron 07:00), løsning or Gosys/Kafka event | SQL "På vent". After a tema change away from AAP the Gosys-vent stays until frist (known behaviour). |
| A job `FEILET` | Processing stopped. A failed `flyt.prosesserBehandling` also blocks saksbehandler actions (`BehandlingUnderProsesseringException`). | Read the latest `feilmelding` (SQL "Jobs"). Fix the cause, then re-run the job (motor API) or wait for `retryFeilede` (07, 12, 15, 20). `POST .../prosesser` does not help: the new job queues behind the failed one (SQL "Jobs blocked by a failed job"). |
| DokumentHåndtering open at DIGITALISER_DOKUMENT / AVKLAR_OVERLEVERING | Waiting for a saksbehandler | none |
| DokumentHåndtering `AVSLUTTET` | Sent to behandlingsflyt if the overlevering vurdering says so | SQL "Active vurderinger"; then look in aap-behandlingsflyt |
| Behandling not avsluttet, no open behov, no `KLAR`/`FEILET` job | Stuck (should not happen) | SQL "Stuck behandlinger", then `POST /api/drift/behandling/{ref}/prosesser` |

Facts that explain many tickets:
- A Journalføring behandling that resumes with the cursor past AVKLAR_TEMA (typically after the AVKLAR_SAK løsning) is rewound to AVKLAR_TEMA and runs it again; the compact steg history shows `AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX`. Not a bug in itself.
- A saksbehandler-solved DIGITALISER_DOKUMENT shows status `AVBRUTT`; look for an `AVSLUTTET` endring in its history.
- Steps are re-run after rewinds and retries, so repeated calls to Joark, Gosys or behandlingsflyt for the same journalpost are expected.
- `IVERKSETTES` behandling status means "in the last steps", not "done". `AVSLUTTET` is done.
- The regelresultat's `system_navn` is the rule outcome. For journalposter with a Fordeling behandling, the decision that was acted on is `avklar_fordeling_vurdering.fagsystem` (`vurdert_av` `Kelvin` = automatic, a NAV ident = manual).

## Codes

Avklaringsbehov `definisjon` column: `1338` DIGITALISER_DOKUMENT, `1339` AVKLAR_TEMA, `1340` AVKLAR_SAK, `1341` AVKLAR_OVERLEVERING, `1342` VENT_PA_GOSYS, `1343` AVKLAR_FORDELING (removed, historical data only), `9001` MANUELT_SATT_PÅ_VENT. Job types: `postmottak-flyt-motor/references/motor-jobs-transactions.md`.

## References

- `references/sql.md`: verified read-only queries: journalpost overview, compact steg history, avklaringsbehov, jobs with errors, fordeling rules, vurderinger, Kelvin saker, person search, på vent, stuck behandlinger, failed and blocked jobs, queue and intake health.
- `RUNBOOK.md` (repo root, Norwegian): operational recipes written by the team.
