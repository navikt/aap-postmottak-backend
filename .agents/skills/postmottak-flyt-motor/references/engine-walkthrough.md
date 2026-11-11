# Engine walkthrough: FlytOrkestrator, StegOrkestrator, BehandlingFlyt

All paths are under `flyt/src/main/kotlin/no/nav/aap/postmottak/` unless stated otherwise. Statements marked **verified** were confirmed by running tests and reading the engine trace.

## 1. Who starts processing

Every execution of steps starts with a `ProsesserBehandlingJobbUtfører` job. These are all production call sites that enqueue it:

| Call site | When |
| --- | --- |
| `mottak/VurderRelevantDokumentForAAPJobbUtfører` | New relevant journalpost: creates the `Fordeling` behandling |
| `forretningsflyt/steg/fordeling/FordelingVideresendSteg` | Fordeling decided KELVIN: creates the `Journalføring` behandling |
| `forretningsflyt/steg/journalføring/VideresendSteg` | Journalført to a Kelvin sak: creates the `DokumentHåndtering` behandling |
| `mottak/KafkaStreams.kt` (`JoarkKafkaHandler`) | New AAP event or tema change for a journalpost with an open Journalføring behandling, or AAP-retur (creates a new Journalføring behandling directly, skipping Fordeling) |
| `avklaringsbehov/AvklaringsbehovOrkestrator.løsAvklaringsbehovOgFortsettProsessering` | After every løsning from the API (`POST /api/behandling/løs-behov`) |
| `prosessering/GjenopptaBehandlingJobbUtfører` | Cron 07:00: behandlinger in `UTREDES` with an open behov whose frist has passed |
| `api/.../BehandlingApi` `GET /api/behandling/{ref}/forbered` | Frontend opens an idle behandling (no activity for 15 min, not avsluttet, no pending job) |
| `api/.../DriftApi` `POST /api/drift/behandling/{ref}/prosesser` | Manual re-trigger by operations |

## 2. ProsesserBehandlingJobbUtfører

```kotlin
val skrivelås = låsRepository.lås(BehandlingId(input.behandlingId()))   // SELECT versjon FROM behandling WHERE id = ? FOR UPDATE
val kontekst = kontroller.opprettKontekst(skrivelås.id)                  // FlytKontekst(journalpostId, behandlingId, type)
kontroller.forberedOgProsesserBehandling(kontekst)
låsRepository.verifiserSkrivelås(skrivelås)                             // UPDATE ... SET versjon = versjon+1 WHERE versjon = old
```

The row lock serialises processing against API mutations (`løs-behov`, `sett-på-vent`, `forbered`) that take the same lock. The versjon bump invalidates any `behandlingVersjon` the frontend holds.

## 3. FlytOrkestrator.forberedBehandling

```text
behandling, avklaringsbehovene = load
avklaringsbehovene.validateTilstand(behandling)          throws UgyldigForespørsel if status == AVSLUTTET
flyt = utledType(type).flyt(); flyt.forberedFlyt(behandling.aktivtSteg())   cursor := current steg

if på vent:                                              any open VENTEPUNKT
    løs every ventepunkt with frist <= today            as SYSTEMBRUKER
    if no longer på vent:
        tilbakefør(flyt.tilbakeflyt(expired))           back to the steg the vent belongs to (no-op if same steg)

førTilbakeTilTidligsteÅpneAvklaringsbehov               open behov in an EARLIER steg than the current one?
                                                         log.error("...er dette en bug") and rewind to it

uendret = informasjonskravGrunnlag.oppdaterFaktagrunnlagForKravliste(flyt.alleFaktagrunnlagFørGjeldendeSteg())
tilbakefør(flyt.tilbakeflytEtterEndringer(uendret))
```

### The informasjonskrav rewind, precisely

- `alleFaktagrunnlagFørGjeldendeSteg()` returns `(steg, krav)` for every steg strictly before the current one. The `oppdaterFaktagrunnlag` flag is always `true` (builder default, never changed).
- `InformasjonskravGrunnlagImpl.oppdaterFaktagrunnlagForKravliste` calls `oppdater()` on each and returns the krav whose result was **not** `ENDRET`:
  ```kotlin
  // Hva gir dette leddet?
  return kravkonstruktører.filterNot { (_, k) -> k.konstruer(...).oppdater(kontekst) == ENDRET }.map { it.second }
  ```
  The interface KDoc says the opposite ("informasjonskrav som har endret seg"). Git history: commit `40cf556e7` (2024-09) replaced `harIkkeGjortOppdateringNå` with `oppdater() == ENDRET` but kept `filterNot`, which inverted the meaning.
- `tilbakeflytEtterEndringer(list)` picks the earliest steg whose kravliste contains any returned krav (excluding steps with status `OPPRETTET`, i.e. START_BEHANDLING) and rewinds there if it is before the current steg.
- `SaksnummerInfoKrav.oppdater` always returns `IKKE_ENDRET`. So in the Journalføring flyt, any resume with the cursor after AVKLAR_TEMA rewinds to AVKLAR_TEMA and re-runs AVKLAR_TEMA, AVKLAR_SAK, ... **Verified**:
  ```text
  [b=1] FlytOrkestrator: Tilbakeført etter oppdatering av registeropplysninger fra 'AVKLAR_SAK' til 'AVKLAR_TEMA'
  [b=1] FlytOrkestrator: Stopper opp ved AVKLAR_TEMA med []
  [b=1] JobbRepository: Planlagt kjøring av jobb[flyt.hendelse] ...      ← extra DokumentflytStoppetHendelse
  [b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA'
  ```
- Even with the "intended" semantics the rewind would usually happen: `JournalpostInformasjonskrav` reports `ENDRET` when Joark's saksnummer/tema differs from the vurdering, which is the normal state until SETT_FAGSAK has run.
- Fordeling and DokumentHåndtering are not affected: no krav before their manual steps.

### tilbakefør

```text
if flyt is empty: return
steg = tilbakeflyt.aktivtSteg()                 the current steg (first element of the reversed sub-flyt)
loop:
    stegOrkestrator.utførTilbakefør(steg)      logs StegTilstand(steg, TILBAKEFØRT). No step code runs.
    steg = tilbakeflyt.neste()                 previous steg; also moves the parent flyt's cursor
    if steg == null:
        loggStopp(); if harHeltStoppet: behandlingHendelseService.stoppet(...)   → flyt.hendelse job
        return
```

`harHeltStoppet = false` only when called from `forberedLøsingAvBehov` (API path). From `forberedBehandling` it always emits a hendelse, even though `prosesserBehandling` continues immediately.

## 4. FlytOrkestrator.prosesserBehandling

```text
behandling, avklaringsbehovene = load fresh; validateTilstand
if på vent: løs expired ventepunkter (no rewind here)
flyt = new flyt; gjeldende = flyt.forberedFlyt(behandling.aktivtSteg())
loop:
    result = stegOrkestrator.utfør(gjeldende, kontekst, behandling, flyt.faktagrunnlagForGjeldendeSteg())
    validerPlassering(flyt, åpne behov)        IllegalStateException if an open behov belongs to an earlier steg
    neste = flyt.neste()                       advances the cursor, even when we are about to stop
    if !result.kanFortsette() || neste == null:
        if neste == null:
            status = AVSLUTTET
            check(no open behov)               "Behandlingen er avsluttet, men det finnes åpne avklaringsbehov."
        behandlingHendelseService.stoppet(fresh behandling)      → StoppetHendelseJobbUtfører (oppgave + statistikk)
        return
    gjeldende = neste
```

`behandling` is one in-memory object shared with `StegOrkestrator`; `loggStegHistorikk` updates it through `behandling.visit(...)`, so `behandling.aktivtSteg()` stays in sync within the job.

## 5. StegOrkestrator.utfør: phases of one steg

`StegStatus.rekkefølge()` = START, OPPDATER_FAKTAGRUNNLAG, UTFØRER, AVKLARINGSPUNKT, AVSLUTTER. For each phase: construct the step (`FlytSteg.konstruer`), run the phase, log a `StegTilstand(steg, phase)` row.

| Phase | Work | Afterwards |
| --- | --- | --- |
| START | none | row, **savepoint** |
| OPPDATER_FAKTAGRUNNLAG | `oppdater()` on this steg's own krav. The result is ignored here. | row, **savepoint** |
| UTFØRER | `steg.utfør(kontekst)`, then handle the transition (below) | row (not written if `utfør` throws) |
| AVKLARINGSPUNKT | open behov with `skalLøsesISteg(this steg)`? → `Stopp`, return | row |
| AVSLUTTER | none, return `Fortsett` | row |

Handling of the UTFØRER result:

| Step returned | Transition | Engine action |
| --- | --- | --- |
| `Fullført` | `Fortsett` | If the behandling has **no** non-vente avklaringsbehov (any status), løs all open ventepunkter ("Har gått videre til nytt steg"). |
| `FantAvklaringsbehov(def)` | `FunnetAvklaringsbehov` (`kanFortsette() = true`) | `avklaringsbehovene.leggTil(def, thisSteg)`: create, reopen if closed, ignore if already open. |
| (none) | `FunnetVentebehov` | Dead branch: no `StegResultat` produces it. `FantAvklaringsbehov` rejects ventepunkt-definisjoner with `require`. |

`utførTilbakefør` only logs `TILBAKEFØRT` (status of that steg's `StegType`), with no construction side effects beyond `konstruer`.

### Logging a StegTilstand

`StegOrkestrator.loggStegHistorikk` → `behandling.visit(tilstand)` (deactivates previous active entry in memory, updates in-memory status) → `BehandlingRepository.loggBesøktSteg` (`UPDATE steg_historikk SET aktiv = false ...` + insert a new active row; unique index allows one active row per behandling) → `oppdaterBehandlingStatus` if the `StegType.status` differs.

Consequences:
- `Behandling.aktivtSteg()` = steg of the active row; `START_BEHANDLING` if there is no history yet.
- A stopped behandling has active row `(steg, AVKLARINGSPUNKT)`; a failed one typically `(steg, OPPDATER_FAKTAGRUNNLAG)`.
- About five rows per steg per pass. Rewinds and resumes multiply this.

## 6. Behandling status

| StegType | Status while the cursor is there |
| --- | --- |
| START_BEHANDLING | OPPRETTET |
| AVKLAR_TEMA, AVKLAR_SAK, SETT_FAGSAK, ENDELIG_JOURNALFØRING, AVKLAR_FORDELING, DIGITALISER_DOKUMENT, OVERLEVER_TIL_FAGSYSTEM | UTREDES |
| VIDERESEND, AVKLAR_FORDELING_VIDERESEND, IVERKSETTES | IVERKSETTES |
| after the last steg | AVSLUTTET (set explicitly by FlytOrkestrator) |

`Status.erAvsluttet()` returns true for IVERKSETTES **and** AVSLUTTET (used for frontend read-only and `/forbered`). `ValiderBehandlingTilstand` only blocks AVSLUTTET. Being på vent does not change status.

## 7. BehandlingFlyt internals

- `BehandlingType.flyt()` builds a **new** `BehandlingFlyt` on every call. It holds an immutable list of `Behandlingsflytsteg(steg, kravliste, oppdaterFaktagrunnlag)` and a **mutable cursor** `aktivtSteg`. Never cache or share an instance.
- `forberedFlyt(stegType)` sets the cursor (throws `IllegalArgumentException("Steg X finnes ikke i flyten")` for unknown steps: this is what happens to in-flight behandlinger if a step is removed without a migration).
- `neste()` moves the cursor forward and returns the next `FlytSteg`, or `null` at the end.
- `tilbakeflyt(...)` / `tilbakeflytEtterEndringer(...)` return a child `BehandlingFlyt` containing `[target .. current]` reversed, with `parent = this`. Calling `neste()` on the child also moves the parent's cursor. A sub-flyt of size ≤ 1 is returned as empty (nothing to do).
- `erStegFør(a, b)` compares list indices; a steg not in the flyt has index -1 and is "before" everything.
- `Avklaringsbehov.løsesISteg()` resolves `StegType.UDEFINERT` (MANUELT_SATT_PÅ_VENT) to `funnetISteg`.
- Leftovers from the behandlingsflyt copy, currently without effect: `ÅrsakTilBehandling` / `kunRelevantVedÅrsakerHvisSattEllersIkke`, frivillige avklaringsbehov (no definisjon is `MANUELT_FRIVILLIG`, so `FrivilligeAvklaringsbehov` adds nothing), `StegType.tekniskSteg` (always false), `kreverToTrinn`, `SENDT_TILBAKE_FRA_BESLUTTER`, `ÅrsakTilRetur`.

## 8. Annotated real trace (manual Journalføring, then automatic DokumentHåndtering)

Produced with `postmottak-testing/scripts/flyt-trace.sh` from `Flyttest."Blir satt på vent for etterspørring av informasjon"` (shortened):

```text
Motor: Plukket jobb [flyt.prosesserBehandling] ... behandlingId = 1            job 1 (test enqueued it)
[b=1] StegOrkestrator: Behandler steg 'START_BEHANDLING'
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA'
[b=1] JournalpostInformasjonskrav: Fant relevante endringer ...                 OPPDATER_FAKTAGRUNNLAG stores the journalpost
[b=1] Avklaringsbehovene: Legger til avklaringsbehov :: AVKLAR_TEMA             created via AvklaringsbehovService
[b=1] FlytOrkestrator: Resultat fra steg: Stopp. Aktivt steg: AVKLAR_TEMA.      AVKLARINGSPUNKT stopped
[b=1] FlytOrkestrator: Neste utledet steg: AVKLAR_SAK.                          cursor moved anyway (harmless)
[b=1] JobbRepository: Planlagt kjøring av jobb[flyt.hendelse] ...               stoppet-hendelse to oppgave/statistikk
[b=-] AvklaringsbehovOrkestrator: Forsøker løse avklaringsbehov[AVKLAR_TEMA]   API/test thread: løsning
[b=-] AvklaringsbehovOrkestrator: Tar av vent for behandling ...                AvklarTemaLøser → taAvVentPgaGosys → extra hendelse
[b=-] JobbRepository: Planlagt kjøring av jobb[flyt.prosesserBehandling] ...    resume job
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA'                             re-runs the steg; vurdering exists now
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_SAK'
[b=1] StegOrkestrator: Fant avklaringsbehov: AVKLAR_SAK(kode='1340')            explicit FantAvklaringsbehov
[b=1] FlytOrkestrator: Stopper opp ved AVKLAR_SAK ...
[b=-] Avklaringsbehovene: Løser avklaringsbehov[AVKLAR_SAK] ...
[b=1] FlytOrkestrator: Tilbakeført ... fra 'AVKLAR_SAK' til 'AVKLAR_TEMA'       informasjonskrav rewind (section 3)
[b=1] StegOrkestrator: Behandler steg 'AVKLAR_TEMA' ... 'AVKLAR_SAK' ... 'SETT_FAGSAK' ... 'ENDELIG_JOURNALFØRING' ... 'VIDERESEND' ... 'IVERKSETTES'
[b=1] FlytOrkestrator: Behandlingen har nådd slutten, avslutter behandling
[b=2] StegOrkestrator: Behandler steg 'START_BEHANDLING'                        DokumentHåndtering created by VideresendSteg
[b=2] StegOrkestrator: Fant avklaringsbehov: DIGITALISER_DOKUMENT(kode='1338')
```
