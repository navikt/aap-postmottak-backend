---
name: postmottak-dokumentflyt
description: Business flow of aap-postmottak-backend - what happens to an incoming AAP journalpost from the Joark Kafka event, through VurderRelevantDokument, the Fordeling, Journalføring and DokumentHåndtering behandlinger, to Kelvin (behandlingsflyt), Arena or Gosys. Explains the intent of each flyt and steg, automatic vs manual decisions per document type, fordeling rules, feature toggles, data scope and the external systems involved. Load when working on business logic or routing, or when explaining why a document ended up where it did.
---

# Postmottak: the journey of a document

If the user or the code contradicts this skill, warn the user and ask whether the skill and/or `AGENTS.md` should be updated. See `AGENTS.md` → "When the user contradicts this documentation".

Postmottak receives incoming documents (journalposter in Joark) on tema AAP and gets each one to the right place:

- **Kelvin** (the new AAP system, aap-behandlingsflyt): journalført on a Kelvin sak, digitised into structured data, delivered as a hendelse.
- **Arena** (the old system): Arena oppgave + automatic journalføring on the Arena sak, or a Gosys journalføringsoppgave.
- **Gosys**: fordeling or journalføring oppgaver for humans when automation is impossible.
- **Another tema**, when a saksbehandler decides the document does not belong to AAP.

How the engine executes this (jobs, steps, avklaringsbehov) is in the `postmottak-flyt-motor` skill.

## Vocabulary

| Term | Meaning |
| --- | --- |
| journalpost | A registered incoming shipment in Joark with one or more dokumenter. Status `MOTTATT` = not yet journalført. |
| SAF / dokarkiv | Read API (GraphQL + REST for files) / write API for Joark. |
| brevkode | Form code of a dokument, e.g. `NAV 11-13.05` søknad, `NAV 08-07.08` legeerklæring (`journalpostogbehandling/journalpost/Brevkoder.kt`). The hoveddokument decides the type. |
| digital vs papir | Digital = original variant is JSON (`Journalpost.erDigital()`). Papir = scanned (kanal `SKAN_*`), PDF only. |
| fagsak / generell sak | Journalføring on a specific sak in a fagsystem (`KELVIN`, `AO01` = Arena) vs on the person's generic sak (`GENERELL_SAK`). |
| Kelvin sak | A sak (saksnummer) in aap-behandlingsflyt. |
| Gosys oppgave | Oppgave in Nav's Oppgave API: `JFR` journalføring, `FDR` fordeling. Different from Kelvin oppgaver (aap-oppgave), which postmottak drives through `DokumentflytStoppetHendelse`. |
| innkommende journalpost | Postmottak's record that a journalpost was received and evaluated (table `INNKOMMENDE_JOURNALPOST`, once per journalpost), including enhet and regelresultat. |
| vurdering | A decision stored per behandling (tema, sak, fordeling, digitalisering, overlevering). |

## End to end

```text
Joark ─Kafka topic teamdokumenthandtering.aapen-dok-journalfoering─► JoarkKafkaHandler (mottak/KafkaStreams.kt)
  keep only journalpostStatus == MOTTATT and mottaksKanal != EESSI
  ├─ temaGammelt == AAP, temaNytt != AAP : open Journalføring behandling? → enqueue processing
  └─ temaNytt == AAP
       ├─ open Journalføring behandling   → take off Gosys-vent, enqueue processing
       ├─ "AAP-retur" (back from another tema after a finished Journalføring that set a non-AAP tema)
       │                                  → new Journalføring behandling directly (no Fordeling)
       └─ otherwise                       → job VurderRelevantDokumentForAAP (forSak(journalpostId))
VurderRelevantDokumentForAAPJobbUtfører (once per journalpost)
  ├─ already recorded / JOURNALFOERT / UTGAAR → record IGNORERT, stop
  ├─ no bruker, or bruker is an orgnr         → Gosys FDR oppgave, record GOSYS_FDR, stop
  └─ else record EVALUERT (+ enhet)           → Fordeling behandling
Fordeling        AVKLAR_FORDELING → AVKLAR_FORDELING_VIDERESEND
  ├─ KELVIN   → new Journalføring behandling (no vurderinger copied)
  ├─ ARENA    → ArenaVideresender: Arena jobs or Gosys JFR; legeerklæring → generell sak tema OPP
  ├─ ARENA_VURDER_11_12 → journalført on the Arena sak without Arena oppgave + Gosys oppgave for 11-12 (stub)
  └─ IGNORERT → nothing (old orgnr cases)
Journalføring    AVKLAR_TEMA → AVKLAR_SAK → SETT_FAGSAK → ENDELIG_JOURNALFØRING → VIDERESEND
  ├─ tema not AAP/OPP → tema changed in Joark (or Gosys-vent), finished
  ├─ OPP or generell sak → journalført on generell sak, finished
  └─ AAP on a Kelvin sak → new DokumentHåndtering behandling (vurderinger copied)
DokumentHåndtering DIGITALISER_DOKUMENT → OVERLEVER_TIL_FAGSYSTEM
  └─ behandlingsflyt.sendHendelse(saksnummer, InnsendingType, Melding) → Kelvin takes over
```

One journalpost normally has up to three behandlinger: Fordeling → Journalføring → DokumentHåndtering. Each is a separate row in `BEHANDLING` with its own steps, avklaringsbehov and referanse (UUID used by the frontend).

A papir søknad through Kafka, as the compact steg-history query in `postmottak-feilsoking/references/sql.md` prints it (verified in a flyt test; S/O/U/A/X are the phases of one pass, T a rewind, an entry ending in A a stop):

```text
Fordeling           START_BEHANDLING:SOUAX > AVKLAR_FORDELING:SOUAX > AVKLAR_FORDELING_VIDERESEND:SOUAX > IVERKSETTES:SOUAX
Journalføring       START_BEHANDLING:SOUAX > AVKLAR_TEMA:SOUASOUAX > AVKLAR_SAK:SOUAT > AVKLAR_TEMA:TSOUAX > AVKLAR_SAK:SOUAX
                    > SETT_FAGSAK:SOUAX > ENDELIG_JOURNALFØRING:SOUAX > VIDERESEND:SOUAX > IVERKSETTES:SOUAX
DokumentHåndtering  START_BEHANDLING:SOUAX > DIGITALISER_DOKUMENT:SOUA        (waiting for a saksbehandler)
```

The Journalføring stopped at AVKLAR_TEMA, resumed after the løsning, stopped at AVKLAR_SAK, and after that løsning was rewound to AVKLAR_TEMA (the informasjonskrav rewind in `postmottak-flyt-motor`) before running to the end. The next behandling is created, with a `flyt.prosesserBehandling` job, by the previous one's videresend step (AVKLAR_FORDELING_VIDERESEND, VIDERESEND).

## Intent of each flyt

| Flyt | Question it answers | Typical manual step |
| --- | --- | --- |
| Fordeling | Should Kelvin or Arena handle this document? Rule-based (`FordelerRegelService`), stored as `AvklarFordelingVurdering`. | None when `PostmottakVurder11_12_gosys` is on: a søknad from a person with løpende ordinær AAP in Arena and < 26 weeks left is routed by the 11-12 vurdering. Manual fordeling (AVKLAR_FORDELING) has been removed; fordeling is always automatic. |
| Journalføring | Make the journalpost correct and final in Joark: confirm tema, choose sak, update and ferdigstill the journalpost, hand over. | AVKLAR_TEMA, AVKLAR_SAK |
| DokumentHåndtering | Turn the document into data Kelvin understands and deliver it. | DIGITALISER_DOKUMENT (kategori + structured JSON), AVKLAR_OVERLEVERING (send to Kelvin or not) |

## What is automatic, per document type (Kelvin path)

| Document | AVKLAR_TEMA | AVKLAR_SAK | DIGITALISER_DOKUMENT | OVERLEVER_TIL_FAGSYSTEM |
| --- | --- | --- | --- | --- |
| Digital søknad | auto AAP | auto `finnEllerOpprettSak` | auto: parse `SøknadV0` from SAF | auto, sent |
| Digital meldekort | auto AAP | auto `finnEllerOpprettSak` | auto: parse `MeldekortV0` | auto; not sent when there is no structured document |
| Digital legeerklæring | auto: AAP if any Kelvin sak not `TRUKKET`, else OPP (→ generell sak, finished) | auto if `tillaterAutomatiskBehandlingAvLegeerklæring()`, else manual | same condition: auto (no content) or manual | same condition: auto `LegeerklæringV0` or manual |
| Klage (toggle `AutomatiskKlageJournalforing`) | auto AAP | auto if exactly one Kelvin sak, else manual | auto `KlageV0(kravMottatt)` if the sak was not newly created | auto, sent |
| Papir søknad and everything else | manual | manual (new sak only for søknad) | manual | auto for kategori SØKNAD, MELDEKORT, KLAGE, allowed LEGEERKLÆRING; otherwise manual |

Automatic paths fall back to the manual avklaringsbehov when parsing fails. `tillaterAutomatiskBehandlingAvLegeerklæring()` (`Saksinfo.kt`) is true when the person has no Kelvin sak, any sak has an open behandling, or any sak has `harRettNåEllerIFramtiden`.

## Feature toggles (Unleash, `flyt/.../unleash/FeatureToggle.kt`)

| Toggle | Effect when on | Value in tests (`FakeUnleash`) |
| --- | --- | --- |
| `PostmottakVurder11_12_gosys` | 11-12 fordeling of Arena søknader (see `references/fordeling-og-arena.md`)| on |
| `AutomatiskKlageJournalforing` | Automatic tema, sak and digitalisering for klage | on |
| `PostmottakVelgTema` | Saksbehandler picks the actual tema; no Gosys-vent; SETT_FAGSAK changes tema in Joark | off |
| `DummyFeature` | example only | throws `NotImplementedError` |

## Environment-dependent rules

`fordeler/regler/Regel.kt` `miljøConfig(prod, dev)`: tests and local runs use the **dev** value. `Aldersregel` (≥ 18 år) and `SøknadRegel` (hoveddokument must be a søknad) are active **only in prod**. So in prod the first document of a person without a Kelvin sak must be a søknad to go to Kelvin; in dev and tests other document types can go to Kelvin too.

## References

- `references/mottak.md`: Kafka filtering and branching, AAP-retur, `VurderRelevantDokumentForAAPJobbUtfører`, enhet.
- `references/fordeling-og-arena.md`: the rules and how `Regelresultat.skalTilKelvin()` combines them, manual fordeling, routing, the Arena job chain.
- `references/journalforing.md`: decision tables for AVKLAR_TEMA, AVKLAR_SAK, SETT_FAGSAK, ENDELIG_JOURNALFØRING, VIDERESEND, and the tema scenarios (Gosys-vent, valgt tema, AAP-retur).
- `references/dokumenthandtering.md`: DIGITALISER_DOKUMENT and OVERLEVER_TIL_FAGSYSTEM in detail, `InnsendingType` and `Melding` mapping.
- `references/data-og-integrasjoner.md`: what is stored per journalpost vs per behandling, tables, every external system with its gateway and env var, cron jobs.
