# Journalføring flyt: step by step

Code: `flyt/src/main/kotlin/no/nav/aap/postmottak/forretningsflyt/steg/journalføring/`. Steps run in this order and every step re-checks the state, because steps re-run after resumes and rewinds (see `postmottak-flyt-motor`).

"Ugyldig" = journalpost status `AVBRUTT`, `FEILREGISTRERT` or `UTGAAR`: every step then returns `Fullført` and the behandling ends without touching Joark.

## Journalpost refresh

`JournalpostInformasjonskrav` runs in the OPPDATER_FAKTAGRUNNLAG phase of AVKLAR_TEMA and on every resume after it. It reads SAF and appends a new `JOURNALPOST` row when anything differs (rows are per journalpostId and shared by all behandlinger of that journalpost). For a journalpost whose bruker is an orgnr it requires status `JOURNALFOERT` and otherwise throws. `SaksnummerInfoKrav` stores a fresh snapshot of the person's Kelvin saker (behandlingsflyt `finnSaker`) when it differs.

## AVKLAR_TEMA (`AvklarTemaSteg`)

First `AvklaringsbehovService` opens, keeps or cancels AVKLAR_TEMA (table in `postmottak-flyt-motor/references/avklaringsbehov.md`). Then, first matching row:

| Situation | Result |
| --- | --- |
| ugyldig | `Fullført` |
| Joark status `JOURNALFOERT` (done elsewhere) | `Fullført`, no vurdering stored |
| no vurdering, Joark tema ≠ AAP | store `(skalTilAap=false, UKJENT)` |
| no vurdering, digital legeerklæring | store `(true, AAP)` if the person has a Kelvin sak that is not `TRUKKET`, else `(false, OPP)` |
| no vurdering, digital søknad, digital meldekort, or klage with `AutomatiskKlageJournalforing` | store `(true, AAP)` |
| no vurdering, anything else | `Fullført`; AVKLAR_TEMA is open, so the flyt stops |
| vurdering UKJENT, Joark tema still AAP (waiting for Gosys) | create a Gosys "endre tema" oppgave if none exists (fallback enhet `0393` when NORG has no arbeidsfordeling) |
| vurdering UKJENT, Joark tema no longer AAP (Gosys done) | ferdigstill the journalpost's open Gosys AAP oppgaver |

Løsning `AvklarTemaLøsning(skalTilAap, tema?)` → `AvklarTemaLøser`: `tema` is only allowed with toggle `PostmottakVelgTema` and must agree with `skalTilAap`. Without `tema` the vurdering is AAP or UKJENT. Tema ≠ AAP with the toggle off puts the behandling on `VENT_PA_GOSYS` (+2 weeks); otherwise any Gosys-vent is removed.

## AVKLAR_SAK (`AvklarSakSteg`)

Tema = the vurdering, or Joark's tema if there is none. First matching row:

| Situation | Result |
| --- | --- |
| ugyldig | `Fullført` |
| `JOURNALFOERT` on a fagsystem other than KELVIN | `Fullført` |
| tema not AAP/OPP | `Fullført` |
| tema OPP | store generell sak |
| `JOURNALFOERT` on KELVIN | store that saksnummer |
| digital søknad, digital meldekort, or digital legeerklæring when `tillaterAutomatiskBehandlingAvLegeerklæring()` | behandlingsflyt `finnEllerOpprettSak(ident, mottattDato)`, store it |
| klage with `AutomatiskKlageJournalforing` and exactly one Kelvin sak | store that sak |
| a saksvurdering exists (from the løsning) | `Fullført` |
| otherwise | `FantAvklaringsbehov(AVKLAR_SAK)` |

The automatic rows come before the "vurdering exists" check, so `finnEllerOpprettSak` is called and a new vurdering stored on every re-run.

Løsning `AvklarSaksnummerLøsning` → `AvklarSakLøser`: either an existing `saksnummer`, `førPåGenerellSak`, or `opprettNySak`. `opprettNySak` fails with `AvslagException` if a Kelvin sak has avslag, and is only allowed for søknad (or brevkode "Ukjent"); it calls `finnEllerOpprettSak` inside the API request and stores `opprettetNy = true`. The løsning can also set `journalposttittel`, `avsenderMottaker` and `dokumenter` (titles), which SETT_FAGSAK writes to Joark. `Saksvurdering`'s `init` validates the combination.

## SETT_FAGSAK (`SettFagsakSteg`)

| Situation | Result |
| --- | --- |
| ugyldig or `JOURNALFOERT` | `Fullført` |
| tema vurdering UKJENT | `Fullført`, nothing changed in Joark |
| tema not AAP/OPP | dokarkiv `endreTema` (only if Joark still says AAP), `Nav-User-Id` = who solved AVKLAR_TEMA |
| generell sak | dokarkiv oppdater: generell sak, tema, title, avsender, dokument titles |
| fagsak | dokarkiv oppdater: Kelvin sak (fagsystem KELVIN), bruker = aktiv ident, tema AAP, title, avsender, dokument titles |

`Nav-User-Id` = who solved AVKLAR_SAK. The avsenderMottaker from the vurdering is ignored for digital kanaler. Without one, `JournalføringService` uses the bruker as avsender when SAF has no avsender id; an orgnr avsender is checked in EREG and becomes `UTL_ORG` (name required) if not found.

## ENDELIG_JOURNALFØRING (`JournalføringSteg`)

ugyldig, already `JOURNALFOERT`, or tema not AAP/OPP → `Fullført`. Otherwise dokarkiv `ferdigstill` with journalførende enhet `9999` (maskinell), `Nav-User-Id` = who solved AVKLAR_SAK.

## VIDERESEND (`VideresendSteg`)

| Situation | Result |
| --- | --- |
| ugyldig | `Fullført` |
| `JOURNALFOERT` on a fagsystem other than KELVIN | `Fullført` |
| tema vurdering `skalTilAap == false`, or generell sak | `Fullført` |
| otherwise | create a `DokumentHåndtering` behandling, copy vurderinger with `GrunnlagKopierer`, enqueue its processing |

## Tema scenarios

| Scenario | What happens |
| --- | --- |
| Normal AAP document | tema AAP → sak → Joark oppdater + ferdigstill → DokumentHåndtering |
| "Not AAP", toggle `PostmottakVelgTema` **off** | vurdering UKJENT + `VENT_PA_GOSYS`; AVKLAR_TEMA reopens and the behandling waits at AVKLAR_TEMA, with a Gosys "endre tema" oppgave. When Gosys moves the journalpost to another tema, the next run cancels AVKLAR_TEMA and closes the Gosys AAP oppgaver, **but the vent stays until frist** (verified); the remaining steps then finish without touching Joark. If the journalpost instead comes back as AAP, Kafka removes the vent and the saksbehandler answers AVKLAR_TEMA again. |
| "Not AAP", toggle **on** | the saksbehandler picks the tema; no vent. SETT_FAGSAK changes the tema in Joark; UKJENT changes nothing; OPP is journalført on generell sak. |
| Journalpost returns to AAP after a finished behandling | AAP-retur in the Kafka handler creates a new Journalføring behandling (see `mottak.md`). |
| Journalført elsewhere while waiting | Steps short-circuit. On a Kelvin sak, AVKLAR_SAK stores that saksnummer and VIDERESEND still creates DokumentHåndtering; on another fagsystem the behandling just ends. |
| Journalpost becomes ugyldig | All steps return `Fullført`, open AVKLAR_TEMA is cancelled, the behandling ends. |
