---
name: postmottak-dokumentflyt
description: Forretningsflyten i aap-postmottak-backend - hva som skjer med en innkommende AAP-journalpost fra Joark Kafka-eventet, via VurderRelevantDokument og behandlingene Fordeling, Journalføring og DokumentHåndtering, til Kelvin (behandlingsflyt), Arena eller Gosys. Forklarer hensikten med hver flyt og hvert steg, automatiske og manuelle avgjørelser per dokumenttype, fordelingsregler, feature toggles, dataomfang og eksterne systemer. Last denne når du jobber med forretningslogikk eller ruting, eller når du skal forklare hvorfor et dokument havnet der det gjorde.
---

# Postmottak: veien til et dokument

Hvis brukeren eller koden motsier denne skillen, må du advare brukeren og spørre om skillen og/eller `AGENTS.md` skal oppdateres. Se `AGENTS.md` → "When the user contradicts this documentation".

Postmottak mottar innkommende dokumenter (journalposter i Joark) på tema AAP og sørger for at hvert av dem kommer til riktig sted:

- **Kelvin** (det nye AAP-systemet, aap-behandlingsflyt): journalført på en Kelvin-sak, digitalisert til strukturerte data og levert som en hendelse.
- **Arena** (det gamle systemet): Arena-oppgave og automatisk journalføring på Arena-saken, eller en journalføringsoppgave i Gosys.
- **Gosys**: fordelings- eller journalføringsoppgaver til mennesker når automatisering ikke er mulig.
- **Et annet tema**, når en saksbehandler avgjør at dokumentet ikke hører hjemme under AAP.

Hvordan motoren kjører dette (jobber, steg, avklaringsbehov) står i skillen `postmottak-flyt-motor`.

## Begreper

| Begrep | Betydning |
| --- | --- |
| journalpost | En registrert innsending i Joark med ett eller flere dokumenter. Status `MOTTATT` = ikke journalført ennå. |
| SAF / dokarkiv | Lese-API (GraphQL, og REST for filer) / skrive-API for Joark. |
| brevkode | Skjemakode for et dokument, f.eks. `NAV 11-13.05` søknad, `NAV 08-07.08` legeerklæring (`journalpostogbehandling/journalpost/Brevkoder.kt`). Hoveddokumentet avgjør typen. |
| digital vs papir | Digital = originalvarianten er JSON (`Journalpost.erDigital()`). Papir = skannet (kanal `SKAN_*`), bare PDF. |
| fagsak / generell sak | Journalføring på en bestemt sak i et fagsystem (`KELVIN`, `AO01` = Arena) mot journalføring på personens generelle sak (`GENERELL_SAK`). |
| Kelvin-sak | En sak (saksnummer) i aap-behandlingsflyt. |
| Gosys-oppgave | Oppgave i Navs Oppgave-API: `JFR` journalføring, `FDR` fordeling. Skiller seg fra Kelvin-oppgaver (aap-oppgave), som postmottak styrer gjennom `DokumentflytStoppetHendelse`. |
| innkommende journalpost | Postmottaks registrering av at en journalpost er mottatt og vurdert (tabellen `INNKOMMENDE_JOURNALPOST`, én gang per journalpost), med enhet og regelresultat. |
| vurdering | En avgjørelse som lagres per behandling (tema, sak, fordeling, digitalisering, overlevering). |

## Fra ende til ende

```text
Joark ─Kafka topic teamdokumenthandtering.aapen-dok-journalfoering─► JoarkKafkaHandler (mottak/KafkaStreams.kt)
  behold bare journalpostStatus == MOTTATT og mottaksKanal != EESSI
  ├─ temaGammelt == AAP, temaNytt != AAP : åpen Journalføring-behandling? → legg prosessering i kø
  └─ temaNytt == AAP
       ├─ åpen Journalføring-behandling   → ta av Gosys-vent, legg prosessering i kø
       ├─ "AAP-retur" (tilbake fra et annet tema etter en ferdig Journalføring som satte et tema som ikke er AAP)
       │                                  → ny Journalføring-behandling direkte (uten Fordeling)
       └─ ellers                          → jobb VurderRelevantDokumentForAAP (forSak(journalpostId))
VurderRelevantDokumentForAAPJobbUtfører (én gang per journalpost)
  ├─ allerede registrert / JOURNALFOERT / UTGAAR → registrer IGNORERT, stopp
  ├─ ingen bruker, eller bruker er et orgnr      → Gosys FDR-oppgave, registrer GOSYS_FDR, stopp
  └─ ellers registrer EVALUERT (+ enhet)         → Fordeling-behandling
Fordeling        AVKLAR_FORDELING → AVKLAR_FORDELING_VIDERESEND
  ├─ KELVIN   → ny Journalføring-behandling (ingen vurderinger kopieres)
  ├─ ARENA    → ArenaVideresender: Arena-jobber eller Gosys JFR; legeerklæring → generell sak med tema OPP
  └─ IGNORERT → ingenting (gamle orgnr-saker)
Journalføring    AVKLAR_TEMA → AVKLAR_SAK → SETT_FAGSAK → ENDELIG_JOURNALFØRING → VIDERESEND
  ├─ tema ikke AAP/OPP → tema endret i Joark (eller Gosys-vent), ferdig
  ├─ OPP eller generell sak → journalført på generell sak, ferdig
  └─ AAP på en Kelvin-sak → ny DokumentHåndtering-behandling (vurderinger kopiert)
DokumentHåndtering DIGITALISER_DOKUMENT → OVERLEVER_TIL_FAGSYSTEM
  └─ behandlingsflyt.sendHendelse(saksnummer, InnsendingType, Melding) → Kelvin tar over
```

En journalpost har vanligvis opptil tre behandlinger: Fordeling → Journalføring → DokumentHåndtering. Hver av dem er en egen rad i `BEHANDLING` med egne steg, avklaringsbehov og referanse (UUID som frontend bruker).

En papirsøknad gjennom Kafka, slik den kompakte steghistorikk-spørringen i `postmottak-feilsoking/references/sql.md` skriver den ut (verifisert i en flyt-test; S/O/U/A/X er fasene i én gjennomkjøring, T er en tilbakespoling, og en oppføring som slutter på A er et stopp):

```text
Fordeling           START_BEHANDLING:SOUAX > AVKLAR_FORDELING:SOUAX > AVKLAR_FORDELING_VIDERESEND:SOUAX > IVERKSETTES:SOUAX
Journalføring       START_BEHANDLING:SOUAX > AVKLAR_TEMA:SOUASOUAX > AVKLAR_SAK:SOUASOUAX
                    > SETT_FAGSAK:SOUAX > ENDELIG_JOURNALFØRING:SOUAX > VIDERESEND:SOUAX > IVERKSETTES:SOUAX
DokumentHåndtering  START_BEHANDLING:SOUAX > DIGITALISER_DOKUMENT:SOUA        (venter på en saksbehandler)
```

Journalføring stoppet ved AVKLAR_TEMA, fortsatte etter løsningen, stoppet ved AVKLAR_SAK og ble etter den løsningen spolt tilbake til AVKLAR_TEMA (tilbakespolingen for informasjonskrav i `postmottak-flyt-motor`) før den kjørte til slutten. Neste behandling opprettes, med en `flyt.prosesserBehandling`-jobb, av videresend-steget i den forrige (AVKLAR_FORDELING_VIDERESEND, VIDERESEND).

## Hensikten med hver flyt

| Flyt | Spørsmålet den svarer på | Typisk manuelt steg |
| --- | --- | --- |
| Fordeling | Skal Kelvin eller Arena behandle dokumentet? Regelbasert (`FordelerRegelService`), lagres som `AvklarFordelingVurdering`. | Ingen. Med toggle `KantTilKantV1` på går også en søknad som er kant-til-kant (rettighet oppbrukt i Arena, maksdato mindre enn 12 uker unna) til Kelvin. De 12 ukene er midlertidige for MVP kant-til-kant og går tilbake til 26 i del 2. Manuell fordeling (AVKLAR_FORDELING) er fjernet. Fordelingen er alltid automatisk. |
| Journalføring | Gjøre journalposten korrekt og endelig i Joark: bekrefte tema, velge sak, oppdatere og ferdigstille journalposten, og sende videre. | AVKLAR_TEMA, AVKLAR_SAK |
| DokumentHåndtering | Gjøre dokumentet om til data Kelvin forstår, og levere det. | DIGITALISER_DOKUMENT (kategori + strukturert JSON), AVKLAR_OVERLEVERING (send til Kelvin eller ikke) |

## Hva som er automatisk, per dokumenttype (Kelvin-løpet)

| Dokument | AVKLAR_TEMA | AVKLAR_SAK | DIGITALISER_DOKUMENT | OVERLEVER_TIL_FAGSYSTEM |
| --- | --- | --- | --- | --- |
| Digital søknad | auto AAP | auto `finnEllerOpprettSak` | auto: parse `SøknadV0` fra SAF | auto, sendt |
| Digital meldekort | auto AAP | auto `finnEllerOpprettSak` | auto: parse `MeldekortV0` | auto; ikke sendt når det ikke finnes strukturert dokument |
| Digital legeerklæring | auto: AAP hvis noen Kelvin-sak ikke er `TRUKKET`, ellers OPP (→ generell sak, ferdig) | auto hvis `tillaterAutomatiskBehandlingAvLegeerklæring()`, ellers manuell | samme vilkår: auto (uten innhold) eller manuell | samme vilkår: auto `LegeerklæringV0` eller manuell |
| Klage (toggle `AutomatiskKlageJournalforing`) | auto AAP | auto hvis nøyaktig én Kelvin-sak, ellers manuell | auto `KlageV0(kravMottatt)` hvis saken ikke er nyopprettet | auto, sendt |
| Papirsøknad og alt annet | manuell | manuell (ny sak bare for søknad) | manuell | auto for kategori SØKNAD, MELDEKORT, KLAGE og tillatt LEGEERKLÆRING; ellers manuell |

Automatiske løp faller tilbake til det manuelle avklaringsbehovet når parsing feiler. `tillaterAutomatiskBehandlingAvLegeerklæring()` (`Saksinfo.kt`) er sann når personen ikke har noen Kelvin-sak, en sak har en åpen behandling, eller en sak har `harRettNåEllerIFramtiden`.

## Feature toggles (Unleash, `flyt/.../unleash/FeatureToggle.kt`)

| Toggle | Effekt når på | Verdi i tester (`FakeUnleash`) |
| --- | --- | --- |
| `KantTilKantV1` | Kant-til-kant-søknader (rettighet oppbrukt i Arena) går til Kelvin. Når av, logges og telles resultatet bare (se `references/fordeling-og-arena.md`) | av |
| `AutomatiskKlageJournalforing` | Automatisk tema, sak og digitalisering for klage | på |
| `PostmottakVelgTema` | Saksbehandler velger selve temaet; ingen Gosys-vent; SETT_FAGSAK endrer tema i Joark | av |
| `DummyFeature` | bare et eksempel | kaster `NotImplementedError` |

## Miljøavhengige regler

`fordeler/regler/Regel.kt` `miljøConfig(prod, dev)`: tester og lokale kjøringer bruker **dev**-verdien. `Aldersregel` (≥ 18 år) og `SøknadRegel` (hoveddokumentet må være en søknad) er aktive **bare i prod**. I prod må derfor det første dokumentet fra en person uten Kelvin-sak være en søknad for å gå til Kelvin. I dev og tester kan andre dokumenttyper også gå til Kelvin.

## Referanser

- `references/mottak.md`: Kafka-filtrering og forgreining, AAP-retur, `VurderRelevantDokumentForAAPJobbUtfører`, enhet.
- `references/fordeling-og-arena.md`: reglene og hvordan `Regelresultat.skalTilKelvin()` kombinerer dem, manuell fordeling, ruting, Arena-jobbkjeden.
- `references/journalforing.md`: beslutningstabeller for AVKLAR_TEMA, AVKLAR_SAK, SETT_FAGSAK, ENDELIG_JOURNALFØRING og VIDERESEND, og temascenarioene (Gosys-vent, valgt tema, AAP-retur).
- `references/dokumenthandtering.md`: DIGITALISER_DOKUMENT og OVERLEVER_TIL_FAGSYSTEM i detalj, mapping av `InnsendingType` og `Melding`.
- `references/data-og-integrasjoner.md`: hva som lagres per journalpost og per behandling, tabeller, hvert eksterne system med gateway og miljøvariabel, cron-jobber.
