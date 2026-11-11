# Avklaringsbehov: definitions, lifecycle, løsning flow, ventepunkter

Paths are under `flyt/src/main/kotlin/no/nav/aap/postmottak/` unless stated otherwise. **Verified** means confirmed by a test run.

## Definitions

`kontrakt/.../avklaringsbehov/Definisjon.kt` (published contract, consumed by the frontend and other apps). Codes are duplicated as `const val *_KODE` and `enum AvklaringsbehovKode` in `AvklaringsbehovKode.kt`; keep them in sync.

| Definisjon | Kode | Type | Løses i steg | Created by | Løsning → Løser |
| --- | --- | --- | --- | --- | --- |
| AVKLAR_FORDELING (deprecated, removed; historical data only) | 1343 | MANUELT_PÅKREVD | AVKLAR_FORDELING | never raised | none (løsning and løser removed) |
| AVKLAR_TEMA | 1339 | MANUELT_PÅKREVD | AVKLAR_TEMA | `AvklarTemaSteg` via `AvklaringsbehovService` | `AvklarTemaLøsning` → `AvklarTemaLøser` |
| AVKLAR_SAK | 1340 | MANUELT_PÅKREVD | AVKLAR_SAK | `AvklarSakSteg` returns `FantAvklaringsbehov` | `AvklarSaksnummerLøsning` → `AvklarSakLøser` |
| DIGITALISER_DOKUMENT | 1338 | MANUELT_PÅKREVD | DIGITALISER_DOKUMENT | `DigitaliserDokumentSteg` | `DigitaliserDokumentLøsning` → `DigitaliserDokumentLøser` |
| AVKLAR_OVERLEVERING | 1341 | MANUELT_PÅKREVD | OVERLEVER_TIL_FAGSYSTEM | `OverleverTilFagsystemSteg` | `AvklarOverleveringLøsning` → `AvklarOverleveringLøser` |
| MANUELT_SATT_PÅ_VENT | 9001 | VENTEPUNKT, default frist 3 weeks | UDEFINERT = the steg it was set in | API `sett-på-vent` | `SattPåVentLøsning` → `SattPåVentLøser` (no-op, "Tatt av vent") |
| VENT_PA_GOSYS | 1342 | VENTEPUNKT, default frist 5 days, but set with +2 weeks | AVKLAR_TEMA | `AvklarTemaLøser` → `AvklaringsbehovOrkestrator.settBehandlingPåVentForTemaEndring` | none: cannot be solved through the API |

All are solved by role `SAKSBEHANDLER_NASJONAL`, none require two-step approval. Code `1337` exists in `AvklaringsbehovKode` but has no definisjon (legacy).

## Statuses and storage

- `AVKLARINGSBEHOV`: one row per (behandling, definisjon) (unique index), with `funnet_i_steg`.
- `AVKLARINGSBEHOV_ENDRING`: append-only history (status, begrunnelse, frist, venteårsak, opprettet_av, tid). Current status = the latest endring by timestamp.
- Open: `OPPRETTET` (and the unused `SENDT_TILBAKE_FRA_BESLUTTER`). Closed: `AVSLUTTET` (solved) or `AVBRUTT` (cancelled).
- `Avklaringsbehovene` (from `AvklaringsbehovRepository.hentAvklaringsbehovene`) reads the database on **every** call (`alle()`, `åpne()` ...) and returns new objects. Change state only through its methods, which persist (`leggTil`, `løsAvklaringsbehov`, `avbrytForSteg`, ...).

State transitions:

| From → to | Caused by |
| --- | --- |
| none → OPPRETTET | `leggTil`: a step returned `FantAvklaringsbehov`, `AvklaringsbehovService`, or sett-på-vent |
| OPPRETTET → AVSLUTTET | `løsAvklaringsbehov`: API løsning, expired frist, `taAvVentPgaGosys`, the vent-closing rule below; `internalAvslutt` from `AvklaringsbehovService` |
| OPPRETTET or AVSLUTTET → AVBRUTT | `avbrytForSteg` (ignores status), `internalAvbryt` from `AvklaringsbehovService` |
| AVSLUTTET or AVBRUTT → OPPRETTET | `leggTil` reopens (for ventepunkter it also updates `funnet_i_steg`) |

## How steps create behov

**Explicit (most steps):** `return FantAvklaringsbehov(Definisjon.X)`. `StegOrkestrator` calls `leggTil(X, currentSteg)`: create if missing, reopen if closed, ignore if already open. The step must return `Fullført` once a vurdering exists, or it will reopen the behov forever.

**Declarative (`AvklarTemaSteg` only):** `AvklaringsbehovService.oppdaterAvklaringsbehov(def, vedtakBehøverVurdering, erTilstrekkeligVurdert, kontekst)` sets the desired state, then the step returns `Fullført`; AVKLARINGSPUNKT stops if the behov is open.

| needs vurdering? | current state | sufficiently vurdert? | Result |
| --- | --- | --- | --- |
| yes | none, `AVBRUTT`, or never solved | (not asked) | OPPRETTET (create/reopen) |
| yes | solved before and now `AVSLUTTET` | yes | unchanged |
| yes | solved before, now `OPPRETTET` | yes | `AVSLUTTET` (`internalAvslutt`) |
| yes | solved before | no | reopen → OPPRETTET |
| no | none or `AVBRUTT` | (not asked) | unchanged |
| no | `OPPRETTET`/`AVSLUTTET` | (not asked) | `AVBRUTT` |

For AVKLAR_TEMA: "needs vurdering" is false for ugyldig, already JOURNALFOERT, tema ≠ AAP, digital søknad/legeerklæring/meldekort and automatic klage. "Sufficiently vurdert" is `false` only when the vurdering is UKJENT while Joark still says AAP and the `PostmottakVelgTema` toggle is off; this is why AVKLAR_TEMA reopens after "skal ikke til AAP" (**verified**).

## The løsning flow, end to end

1. `POST /api/behandling/løs-behov` with `{ referanse, behandlingVersjon, behov: { behovstype: "1339", ... } }` (`api/.../flyt/AvklaringsbehovApi.kt`). `behovstype` selects the `AvklaringsbehovLøsning` subtype through `@JsonTypeName`; subtypes are registered in `App.kt` with `registerSubtypes(utledSubtypes())`. Authorization: operasjon SAKSBEHANDLE on the journalpost, role from `definisjon.løsesAv`.
2. In one transaction: lock the behandling row, then `BehandlingTilstandValidator`: status ≠ AVSLUTTET, `behandlingVersjon` matches (`OutdatedBehandlingException`), and no `flyt.prosesserBehandling` job in `KLAR` **or `FEILET`** for the behandling (`BehandlingUnderProsesseringException`).
3. `AvklaringsbehovHendelseHåndterer` → `ValiderBehandlingTilstand`: the behov must exist on the behandling (any status), and its steg must not be after the current steg.
4. `AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering`:
   1. `flytOrkestrator.forberedLøsingAvBehov(def)`: rewind to the steg where the behov is solved (no hendelse emitted), then require the cursor to be exactly there. Re-solving an earlier behov (e.g. AVKLAR_TEMA while at AVKLAR_SAK) is therefore allowed and rewinds the behandling.
   2. `løsning.løs(...)` → løser stores the vurdering (may call other systems, e.g. behandlingsflyt).
   3. `avklaringsbehovene.løsAvklaringsbehov(def, begrunnelse, bruker)` → `AVSLUTTET`.
   4. Enqueue `ProsesserBehandlingJobbUtfører`.
5. Bump versjon, commit, respond `202 Accepted`. The frontend polls `GET /api/behandling/{ref}/flyt` (prosessering status `JOBBER`/`FEILET`/`FERDIG` based on pending jobs).

A løser that throws (most validation is `require`) rolls everything back.

## Ventepunkter

| | MANUELT_SATT_PÅ_VENT | VENT_PA_GOSYS |
| --- | --- | --- |
| Set by | `POST /api/behandling/{ref}/sett-på-vent` → `settBehandlingPåVent` (frist from request, else +3 weeks) | `AvklarTemaLøser` when tema ≠ AAP and `PostmottakVelgTema` is off (frist +2 weeks, grunn `VENTER_PÅ_BEHANDLING_I_GOSYS`) |
| Belongs to steg | the behandling's current steg (`funnet_i_steg`) | AVKLAR_TEMA |
| Starts processing? | no (only a hendelse) | no; the løsning job does, and stops again at AVKLAR_TEMA |
| Removed by | løsning `SattPåVentLøsning` (behovstype 9001), or frist | `taAvVentPgaGosys` (AvklarTemaLøser with AAP or with the toggle on; Kafka AAP event for a journalpost with an open Journalføring behandling), or frist |

Common rules:
- Frist expiry: cron `GjenopptaBehandlingJobbUtfører` (07:00) enqueues processing for `UTREDES` behandlinger whose behov has a latest endring `OPPRETTET` with `frist <= today`. `FlytOrkestrator` then solves expired ventepunkter as SYSTEMBRUKER and rewinds to their steg.
- A step returning `Fullført` closes open ventepunkter **only if the behandling has no non-vente avklaringsbehov in any status**. **Verified consequence:** after Gosys moves the journalpost to another tema, AVKLAR_TEMA becomes `AVBRUTT`, but `VENT_PA_GOSYS` stays open until frist, so the Journalføring behandling waits up to two weeks before it finishes.
- `AVKLARINGSPUNKT` stops on an open ventepunkt in the current steg just like on any other behov.
- Being på vent does not change the behandling status.
- Only three `ÅrsakTilSettPåVent` values work end to end: `VENTER_PÅ_OPPLYSNINGER`, `VENTER_PÅ_SVAR_FRA_BRUKER`, `VENTER_PÅ_BEHANDLING_I_GOSYS`. The API and the kontrakt accept six, but `BehandlingHendelseServiceImpl` throws `error("Skal ikke kunne skje")` for the others when building the hendelse, which fails the request. Update that mapping when adding or enabling a reason.

## Outgoing hendelse (`DokumentflytStoppetHendelse`)

`BehandlingHendelseServiceImpl.stoppet` enqueues `StoppetHendelseJobbUtfører` (`flyt.hendelse`), which sends it to aap-oppgave (`OppgaveGateway.varsleHendelse`, creates and closes Kelvin oppgaver) and aap-statistikk. Emitted when processing stops or ends, on every rewind from `forberedBehandling`, on sett-på-vent, and on every `AvklarTemaLøser` run. Consumers must therefore tolerate repeated hendelser with the same state.

- `aktivtAvklaringsbehov`: first non-vente behov (sorted by flyt order of `funnetISteg`) that is open in the current steg.
- `avklaringsbehov`: all behov with their full history.
- If neither a journalpost nor any behov exists for the behandling, nothing is sent (warn log). If behov exist but no journalpost, it throws.

## Other things that surprise people

- `avbrytForSteg(steg)` cancels **every** non-vente behov of that steg regardless of status. `DigitaliserDokumentSteg` calls it whenever a vurdering exists, so a saksbehandler-solved DIGITALISER_DOKUMENT ends as `AVBRUTT` (**verified**: history `OPPRETTET, AVSLUTTET, AVBRUTT`). `OverleverTilFagsystemSteg` cancels DIGITALISER_DOKUMENT (not its own steg) for ugyldige journalposter.
- `hvemSomLøste(def)` returns the `endretAv` of the latest `AVSLUTTET` endring. `SettFagsakSteg` and `JournalføringSteg` send it to Joark as `Nav-User-Id` (who journalførte).
- `leggTil` on an existing open behov is a no-op (logs "Forsøkte å legge til et avklaringsbehov som allerede eksisterte").
