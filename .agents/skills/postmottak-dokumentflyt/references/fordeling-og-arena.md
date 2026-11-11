# Fordeling (Kelvin or Arena) and the Arena path

Code: `flyt/src/main/kotlin/no/nav/aap/fordeler/` (rules, Arena) and `flyt/.../postmottak/forretningsflyt/steg/fordeling/`.

## AVKLAR_FORDELING (`AvklarFordelingSteg`)

1. A vurdering already exists → `Fullført`.
2. No innkommende journalpost: allowed only when the SAF bruker is an orgnr (old behandlinger); stores `IGNORERT` and finishes. Otherwise `IllegalArgumentException`.
3. Regelresultat: reuse the stored one, or evaluate the rules once and store it on the innkommende journalpost.
4. Store `KELVIN` if `regelresultat.skalTilKelvin()`. Else, when `PostmottakVurder11_12_gosys` is on and the hoveddokument is a søknad, store the result of `Vurdering11_12Service.vurderFordeling` (below). Else `ARENA`. All vurdert av SYSTEMBRUKER. The step never raises an avklaringsbehov: manual fordeling (Definisjon AVKLAR_FORDELING, 1343) has been removed and the definisjon is deprecated, kept only for historical rows.

### 11-12 fordeling (`fordeler/arena/Vurdering11_12Service.kt`)

A søknad from a person with significant Arena history and løpende ordinær AAP (§§ 11-5/11-6) is either a forlengelse of the Arena sak (§ 11-12, Arena) or a new sak (Kelvin). Evaluated in order:

| Condition | Result |
| --- | --- |
| `ArenaService.kanFordelesAutomatiskTilKelvinPga11_12_erMakset` | `KELVIN` |
| No løpende ordinær AAP: no sak, sak avsluttet, not `erLopende()`, aktfase UVUP/FA/SPE, 11-12 already innvilget, or maksdato passed | `ARENA` |
| Maksdato (`maxdatoOrdinaer`, fallback `maxdatoAap`) ≥ mottatt dato + 26 weeks | `ARENA` |
| `ArenaService.statusPå11_12Vurdering` = true (forlengelse) | `ARENA` |
| `statusPå11_12Vurdering` = false (ny sak) | `KELVIN` |
| `statusPå11_12Vurdering` = null (not vurdert) | `ARENA_VURDER_11_12` |

`statusPå11_12Vurdering` is a stub that returns null until implemented. Arena failures throw, so the step fails and the job retries. In tests the status is injected through the `Vurdering11_12Service` constructor.

## Rules (`fordeler/regler/`)

| Regel | Active prod / dev+test | `true` means | Data source |
| --- | --- | --- | --- |
| `KelvinSakRegel` | yes / yes | the person has at least one Kelvin sak | behandlingsflyt `finnSaker` |
| `ArenaSakRegel` | yes / yes | the person has AAP history in Arena. Logged only, not used in the decision. | arenaoppslag |
| `ArenaHistorikkRegel` | yes / yes | **no** significant AAP history in Arena | arenaoppslag |
| `ErIkkeReisestønadRegel` | yes / yes | hoveddokument is not a reisestønad søknad or ettersendelse | brevkode |
| `ErIkkeAnkeRegel` | yes / yes | hoveddokument is not an anke | brevkode |
| `ManueltOverstyrtTilArenaRegel` | yes / yes | one of the person's identer is overridden to Arena in table `MANUELL_FORDELING` | database |
| `Aldersregel` | yes / **no** | the person is at least 18 (false if the fødselsdato is unknown) | PDL |
| `SøknadRegel` | yes / **no** | hoveddokument is a søknad | brevkode |

New rules: implement `Regel<T>` with a `companion object : RegelFactory<T>` (`erAktiv`, `medDataInnhenting`). `FordelerRegelService` finds them through the sealed hierarchy. If you add or deactivate a rule that `Regelresultat.skalTilKelvin()` names explicitly, update that function too (comments in the rules say so).

### `Regelresultat.skalTilKelvin()`

1. `ManueltOverstyrtTilArenaRegel` true → **Arena**.
2. `KelvinSakRegel && ErIkkeReisestønadRegel && ErIkkeAnkeRegel` → **Kelvin** (existing Kelvin sak wins over Arena history, age and document type).
3. Otherwise **Kelvin** only if every rule except `KelvinSakRegel`, `ArenaSakRegel` and `ManueltOverstyrtTilArenaRegel` is true; else **Arena**. In prod this means: adult, søknad, no significant Arena history, not reisestønad, not anke.

Notes:
- `FordelerRegelService.evaluer` calls `vurder()` twice per rule (once for a metric, once for the result), so each rule's lookups happen twice.
- The stored regelresultat (`REGELSETT_RESULTAT.SYSTEM_NAVN`) is the rule outcome. The decision is `AVKLAR_FORDELING_VURDERING`; historical manual fordelinger may differ from the regelresultat.

## AVKLAR_FORDELING_VIDERESEND (`FordelingVideresendSteg`)

| Vurdering | Action |
| --- | --- |
| `KELVIN` | Create a `Journalføring` behandling and enqueue processing. Nothing is copied from the Fordeling behandling. |
| `ARENA` | `ArenaVideresender.videresendJournalpostTilArena` |
| `ARENA_VURDER_11_12` | `ArenaVideresender.videresendSøknadForVurdering11_12`: job `JournalførSøknadForVurdering11_12JobbUtfører` (same JOURNALFOERT/UTGAAR/enhet checks as above) |
| `IGNORERT` or none | log only |
| `BEGGE` | throws: not supported yet |

## Arena path (`fordeler/arena/ArenaVideresender.kt`, `fordeler/arena/jobber/`)

`ArenaVideresender` re-reads the journalpost from SAF and does nothing if it is `JOURNALFOERT` or `UTGAAR`. Without an enhet on the innkommende journalpost it goes straight to manual journalføring. Otherwise, by hoveddokument brevkode:

| Brevkode | Action |
| --- | --- |
| LEGEERKLÆRING | Synchronously journalført on generell sak with tema **OPP**, then ferdigstilt (no Arena oppgave) |
| SØKNAD | job `SendSøknadTilArenaJobbUtfører` |
| STANDARD_ETTERSENDING, SØKNAD_OM_REISESTØNAD | job `OppprettOppgaveIArenaJobbUtfører` |
| SØKNAD_OM_REISESTØNAD_ETTERSENDELSE | job `SendTilArenaKjørelisteBehandling` (comment in code: does not work today, missing XML) |
| anything else | job `ManuellJournalføringJobbUtfører` |

All Arena jobs are keyed `forSak(journalpostId)` with an `ArenaVideresenderKontekst` JSON payload, extend `ArenaJobbutførerBase` (skip silently unless the journalpost is still `MOTTATT`) and fall back to `ManuellJournalføringJobbUtfører` instead of failing:

```text
SendSøknadTilArena:      no enhet, or Arena already has an active sak for the person → Manuell
                         else opprett STARTVEDTAK oppgave in Arena → AutomatiskJournalføring(arenaSakId)
OppprettOppgaveIArena:   nyeste aktive Arena-sak → opprett BEHENVPERSON oppgave → AutomatiskJournalføring
SendTilArenaKjøreliste:  behandleKjoerelisteOgOpprettOppgave → AutomatiskJournalføring
JournalførSøknadForVurdering11_12: nyesteAktiveSak → AutomatiskJournalføring(arenaSakId), no Arena oppgave,
                         then Vurdering11_12Service.opprettGosysOppgaveFor11_12Vurdering (stub);
                         no aktiv sak or ≥ 3 retries → Manuell
AutomatiskJournalføring: journalfør on the Arena fagsak (fagsystem AO01) → ferdigstill → close Gosys AAP oppgaver
ManuellJournalføring:    enhet known and < 3 earlier failures → Gosys JFR oppgave for the enhet; otherwise Gosys FDR oppgave
```

Retry limits and fallbacks are listed in `postmottak-flyt-motor/references/motor-jobs-transactions.md`. Arena web service calls go through aap-fss-proxy (`ArenaWebservicesGateway`); Arena reads go through aap-arenaoppslag (`ArenaoppslagGateway`).

## Safety net: `JoarkAvstemmerJobbUtfører` (cron 03:00)

`JoarkAvstemmer.avstem()` fetches AAP journalposter still `MOTTATT` after 5 days from dokarkiv (excluding EESSI) and checks each against the stored regelresultat:

| Situation | Action |
| --- | --- |
| No regelresultat and a Gosys JFR oppgave exists | nothing |
| No regelresultat, created before 2025-04-01 | nothing (pre-Kelvin) |
| No regelresultat | Gosys FDR oppgave, counted as uavstemt |
| Regelresultat KELVIN | counted as uavstemt, no action (error log in prod) |
| Regelresultat ARENA, no Gosys oppgave | Gosys JFR oppgave (retry without enhet and behandlingstema, fallback FDR) |

`GET /api/dokumenter/finn-ubehandlede` lists the uavstemte Kelvin journalposter with the referanse of their open Journalføring behandling.
