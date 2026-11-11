# DokumentHåndtering flyt: digitalisering and overlevering to Kelvin

Code: `flyt/src/main/kotlin/no/nav/aap/postmottak/forretningsflyt/steg/dokumentflyt/`. Created by `VideresendSteg` after journalføring on a Kelvin sak, with the Journalføring behandling's vurderinger copied by `GrunnlagKopierer` (in practice the tema vurdering, the saksvurdering and the Kelvin saker snapshot). This flyt has **no informasjonskrav**: it reads the journalpost stored by the earlier behandling and fails in DIGITALISER_DOKUMENT if none exists (verified).

## DIGITALISER_DOKUMENT (`DigitaliserDokumentSteg`)

Goal: a `Digitaliseringsvurdering` = kategori (`InnsendingType`) + structured JSON (`strukturertDokument`) + optional søknadsdato.

| Situation (first match) | Result |
| --- | --- |
| a vurdering exists | `avbrytForSteg(DIGITALISER_DOKUMENT)` then `Fullført`. This also turns a behov solved by a saksbehandler into `AVBRUTT` (verified). |
| ugyldig | cancel the behov, `Fullført` |
| digital søknad or digital meldekort | fetch the original JSON from SAF, parse it, store the vurdering |
| digital legeerklæring and `tillaterAutomatiskBehandlingAvLegeerklæring()` | store kategori LEGEERKLÆRING without content |
| klage with `AutomatiskKlageJournalforing`, a saksvurdering exists and it was not newly created | store KLAGE with `KlageV0(kravMottatt = mottattDato)` |
| otherwise | `FantAvklaringsbehov(DIGITALISER_DOKUMENT)` |

- Kategori for the automatic path comes from the hoveddokument brevkode (`BrevkoderHelper.mapTilInnsendingType`: søknad, legeerklæring, meldekort and meldekort-korrigering, klage). An unmapped brevkode or a parse error (`DeserializationException`, `IllegalArgumentException`) falls back to the manual behov.
- `Journalpost.finnOriginal()` takes the first dokument with an ORIGINAL variant, not necessarily the hoveddokument (there is a TODO about this).
- An earlier avslag on the person's Kelvin sak only produces a warning log here.

Løsning `DigitaliserDokumentLøsning(kategori, strukturertDokument, søknadsdato)` → `DigitaliserDokumentLøser`:
- `søknadsdato` is required for SØKNAD and MELDEKORT and forbidden for other kategorier; it cannot be after the journalpost's mottatt dato.
- KLAGE requires a saksvurdering that was not newly created.
- `strukturertDokument` is parsed with `DokumentTilMeldingParser` (`gateway/BehandlingsflytGateway.kt`) and stored re-serialised, with `digitalisertManueltGjennomPostmottak = true`.

| InnsendingType | Melding class | Document required |
| --- | --- | --- |
| SØKNAD | `SøknadV0` | yes |
| MELDEKORT | `MeldekortV0` | no |
| ANNET_RELEVANT_DOKUMENT | `AnnetRelevantDokument` (V0/V1) | yes |
| LEGEERKLÆRING | `LegeerklæringV0` | no |
| NY_ÅRSAK_TIL_BEHANDLING | `NyÅrsakTilBehandlingV0` | yes |
| KLAGE | `KlageV0` | yes |
| other types | none | |

The Melding classes come from `no.nav.aap.behandlingsflyt:kontrakt` (version in `gradle/libs.versions.toml`); their JSON shape is owned by aap-behandlingsflyt.

## OVERLEVER_TIL_FAGSYSTEM (`OverleverTilFagsystemSteg`)

Goal: decide whether Kelvin should receive the document, and send it.

| Situation | Result |
| --- | --- |
| ugyldig | cancels the DIGITALISER_DOKUMENT behov (not its own), `Fullført` |
| no overlevering vurdering and kategori SØKNAD, MELDEKORT, KLAGE, or LEGEERKLÆRING when automatic legeerklæring is allowed | automatic vurdering: send, except a MELDEKORT without structured document (already registered manually in Kelvin) |
| still no vurdering | `FantAvklaringsbehov(AVKLAR_OVERLEVERING)` |
| vurdering says send | `BehandlingsflytGateway.sendHendelse(...)`, then `Fullført` |
| vurdering says do not send | `Fullført` |

Løsning `AvklarOverleveringLøsning(skalOverleveres, begrunnelse?)` → `AvklarOverleveringLøser` stores it.

`sendHendelse` arguments:
- `saksnummer` from the saksvurdering (required, `!!`),
- `mottattDato` = søknadsdato (start of day) if set, else the journalpost's `mottattTid`, else its mottatt dato,
- `innsendingstype` = kategori, `kanal` = papir for `SKAN_*` kanaler, otherwise digital,
- `melding` from `utledMelding`: LEGEERKLÆRING → `LegeerklæringV0(beskrivelse = begrunnelse)`; otherwise the parsed structured document, with the overlevering begrunnelse filled into an empty begrunnelse or beskrivelse (`AnnetRelevantDokumentV0` is upgraded to V1 for this),
- `digitalisertIPostmottak` = whether it was digitised manually.

aap-behandlingsflyt receives an `Innsending` with referanse type JOURNALPOST and the journalpostId. The call can be repeated on retry, so the receiver must tolerate duplicates.

## Frontend visibility

`DynamiskStegGruppeVisningService`: the DIGITALISER group is hidden for digital søknad, legeerklæring and meldekort (`DigitaliserVisningUtleder`); OVERLEVER_TIL_FAGSYSTEM is always shown because its group is marked obligatory (its utleder is never consulted).
