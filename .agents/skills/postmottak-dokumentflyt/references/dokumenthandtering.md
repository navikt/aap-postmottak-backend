# DokumentHåndtering-flyt: digitalisering og overlevering til Kelvin

Kode: `flyt/src/main/kotlin/no/nav/aap/postmottak/forretningsflyt/steg/dokumentflyt/`. Opprettes av `VideresendSteg` etter journalføring på en Kelvin-sak, med vurderingene fra Journalføring-behandlingen kopiert av `GrunnlagKopierer` (i praksis tema-vurderingen, saksvurderingen og snapshotet av Kelvin-saker). Denne flyten har **ingen informasjonskrav**: den leser journalposten som den tidligere behandlingen lagret, og feiler i DIGITALISER_DOKUMENT hvis ingen finnes (verifisert).

## DIGITALISER_DOKUMENT (`DigitaliserDokumentSteg`)

Mål: en `Digitaliseringsvurdering` = kategori (`InnsendingType`) + strukturert JSON (`strukturertDokument`) + valgfri søknadsdato.

| Situasjon (første treff) | Resultat |
| --- | --- |
| en vurdering finnes | `avbrytForSteg(DIGITALISER_DOKUMENT)`, deretter `Fullført`. Dette gjør også et behov som en saksbehandler har løst, til `AVBRUTT` (verifisert). |
| ugyldig | kanseller behovet, `Fullført` |
| digital søknad eller digitalt meldekort | hent original-JSON fra SAF, parse den, lagre vurderingen |
| digital legeerklæring og `tillaterAutomatiskBehandlingAvLegeerklæring()` | lagre kategori LEGEERKLÆRING uten innhold |
| klage med `AutomatiskKlageJournalforing`, en saksvurdering finnes og saken er ikke nyopprettet | lagre KLAGE med `KlageV0(kravMottatt = mottattDato)` |
| ellers | `FantAvklaringsbehov(DIGITALISER_DOKUMENT)` |

- Kategorien for det automatiske løpet kommer fra brevkoden til hoveddokumentet (`BrevkoderHelper.mapTilInnsendingType`: søknad, legeerklæring, meldekort og meldekort-korrigering, klage). En brevkode uten mapping eller en parsefeil (`DeserializationException`, `IllegalArgumentException`) faller tilbake til det manuelle behovet.
- `Journalpost.finnOriginal()` tar det første dokumentet med en ORIGINAL-variant, ikke nødvendigvis hoveddokumentet (det er en TODO om dette).
- Et tidligere avslag på personens Kelvin-sak gir bare en advarsel i loggen her.

Løsning `DigitaliserDokumentLøsning(kategori, strukturertDokument, søknadsdato)` → `DigitaliserDokumentLøser`:
- `søknadsdato` er påkrevd for SØKNAD og MELDEKORT og forbudt for andre kategorier. Den kan ikke ligge etter journalpostens mottattdato.
- KLAGE krever en saksvurdering som ikke er nyopprettet.
- `strukturertDokument` parses med `DokumentTilMeldingParser` (`gateway/BehandlingsflytGateway.kt`) og lagres serialisert på nytt, med `digitalisertManueltGjennomPostmottak = true`.

| InnsendingType | Melding-klasse | Dokument påkrevd |
| --- | --- | --- |
| SØKNAD | `SøknadV0` | ja |
| MELDEKORT | `MeldekortV0` | nei |
| ANNET_RELEVANT_DOKUMENT | `AnnetRelevantDokument` (V0/V1) | ja |
| LEGEERKLÆRING | `LegeerklæringV0` | nei |
| NY_ÅRSAK_TIL_BEHANDLING | `NyÅrsakTilBehandlingV0` | ja |
| KLAGE | `KlageV0` | ja |
| andre typer | ingen | |

Melding-klassene kommer fra `no.nav.aap.behandlingsflyt:kontrakt` (versjon i `gradle/libs.versions.toml`). JSON-formen eies av aap-behandlingsflyt.

## OVERLEVER_TIL_FAGSYSTEM (`OverleverTilFagsystemSteg`)

Mål: avgjøre om Kelvin skal motta dokumentet, og sende det.

| Situasjon | Resultat |
| --- | --- |
| ugyldig | kansellerer DIGITALISER_DOKUMENT-behovet (ikke sitt eget), `Fullført` |
| ingen overleveringsvurdering og kategori SØKNAD, MELDEKORT, KLAGE, eller LEGEERKLÆRING når automatisk legeerklæring er tillatt | automatisk vurdering: send, unntatt MELDEKORT uten strukturert dokument (allerede registrert manuelt i Kelvin) |
| fortsatt ingen vurdering | `FantAvklaringsbehov(AVKLAR_OVERLEVERING)` |
| vurderingen sier send | `BehandlingsflytGateway.sendHendelse(...)`, deretter `Fullført` |
| vurderingen sier ikke send | `Fullført` |

Løsning `AvklarOverleveringLøsning(skalOverleveres, begrunnelse?)` → `AvklarOverleveringLøser` lagrer den.

Argumenter til `sendHendelse`:
- `saksnummer` fra saksvurderingen (påkrevd, `!!`),
- `mottattDato` = søknadsdato (start på dagen) hvis satt, ellers journalpostens `mottattTid`, ellers dens mottattdato,
- `innsendingstype` = kategori, `kanal` = papir for `SKAN_*`-kanaler, ellers digital,
- `melding` fra `utledMelding`: LEGEERKLÆRING → `LegeerklæringV0(beskrivelse = begrunnelse)`. Ellers det parsede strukturerte dokumentet, med overleveringsbegrunnelsen fylt inn i en tom begrunnelse eller beskrivelse (`AnnetRelevantDokumentV0` oppgraderes til V1 for dette),
- `digitalisertIPostmottak` = om det ble digitalisert manuelt.

aap-behandlingsflyt mottar en `Innsending` med referansetype JOURNALPOST og journalpostId. Kallet kan gjentas ved retry, så mottakeren må tåle duplikater.

## Synlighet i frontend

`DynamiskStegGruppeVisningService`: DIGITALISER-gruppen er skjult for digital søknad, legeerklæring og meldekort (`DigitaliserVisningUtleder`). OVERLEVER_TIL_FAGSYSTEM vises alltid fordi gruppen er markert som obligatorisk (utlederen konsulteres aldri).
