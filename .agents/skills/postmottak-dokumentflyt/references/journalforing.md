# Journalføring-flyt: steg for steg

Kode: `flyt/src/main/kotlin/no/nav/aap/postmottak/forretningsflyt/steg/journalføring/`. Stegene kjører i denne rekkefølgen, og hvert steg sjekker tilstanden på nytt, fordi steg kjøres om igjen etter gjenopptak og tilbakespoling (se `postmottak-flyt-motor`).

"Ugyldig" = journalpoststatus `AVBRUTT`, `FEILREGISTRERT` eller `UTGAAR`: da returnerer hvert steg `Fullført`, og behandlingen avsluttes uten å røre Joark.

## Oppfriskning av journalpost

`JournalpostInformasjonskrav` kjører i fasen OPPDATER_FAKTAGRUNNLAG i AVKLAR_TEMA og ved hvert gjenopptak etter det. Den leser SAF og legger til en ny `JOURNALPOST`-rad når noe er annerledes (radene er per journalpostId og deles av alle behandlinger av den journalposten). For en journalpost der bruker er et orgnr krever den status `JOURNALFOERT` og kaster ellers unntak. `SaksnummerInfoKrav` lagrer et nytt snapshot av personens Kelvin-saker (behandlingsflyt `finnSaker`) når det er annerledes.

## AVKLAR_TEMA (`AvklarTemaSteg`)

Først åpner, beholder eller kansellerer `AvklaringsbehovService` AVKLAR_TEMA (tabell i `postmottak-flyt-motor/references/avklaringsbehov.md`). Deretter gjelder første rad som passer:

| Situasjon | Resultat |
| --- | --- |
| ugyldig | `Fullført` |
| Joark-status `JOURNALFOERT` (gjort andre steder) | `Fullført`, ingen vurdering lagres |
| ingen vurdering, Joark-tema ≠ AAP | lagre `(skalTilAap=false, UKJENT)` |
| ingen vurdering, digital legeerklæring | lagre `(true, AAP)` hvis personen har en Kelvin-sak som ikke er `TRUKKET`, ellers `(false, OPP)` |
| ingen vurdering, digital søknad, digitalt meldekort, eller klage med `AutomatiskKlageJournalforing` | lagre `(true, AAP)` |
| ingen vurdering, alt annet | `Fullført`; AVKLAR_TEMA er åpent, så flyten stopper |
| vurdering UKJENT, Joark-tema fortsatt AAP (venter på Gosys) | opprett en Gosys-oppgave for å endre tema hvis det ikke finnes en (reserveenhet `0393` når NORG ikke har noen arbeidsfordeling) |
| vurdering UKJENT, Joark-tema ikke lenger AAP (Gosys ferdig) | ferdigstill journalpostens åpne Gosys AAP-oppgaver |

Løsning `AvklarTemaLøsning(skalTilAap, tema?)` → `AvklarTemaLøser`: `tema` er bare tillatt med toggle `PostmottakVelgTema` og må stemme med `skalTilAap`. Uten `tema` er vurderingen AAP eller UKJENT. Tema ≠ AAP med toggle av setter behandlingen på `VENT_PA_GOSYS` (+2 uker). Ellers fjernes all Gosys-vent.

## AVKLAR_SAK (`AvklarSakSteg`)

Tema = vurderingen, eller Joarks tema hvis det ikke finnes noen vurdering. Første rad som passer gjelder:

| Situasjon | Resultat |
| --- | --- |
| ugyldig | `Fullført` |
| `JOURNALFOERT` på et annet fagsystem enn KELVIN | `Fullført` |
| tema ikke AAP/OPP | `Fullført` |
| tema OPP | lagre generell sak |
| `JOURNALFOERT` på KELVIN | lagre det saksnummeret |
| digital søknad, digitalt meldekort, eller digital legeerklæring når `tillaterAutomatiskBehandlingAvLegeerklæring()` | behandlingsflyt `finnEllerOpprettSak(ident, mottattDato)`, lagre resultatet |
| klage med `AutomatiskKlageJournalforing` og nøyaktig én Kelvin-sak | lagre den saken |
| en saksvurdering finnes (fra løsningen) | `Fullført` |
| ellers | `FantAvklaringsbehov(AVKLAR_SAK)` |

De automatiske radene kommer før sjekken «vurdering finnes», så `finnEllerOpprettSak` kalles og en ny vurdering lagres ved hver ny kjøring.

Løsning `AvklarSaksnummerLøsning` → `AvklarSakLøser`: enten et eksisterende `saksnummer`, `førPåGenerellSak` eller `opprettNySak`. `opprettNySak` feiler med `AvslagException` hvis en Kelvin-sak har avslag, og er bare tillatt for søknad (eller brevkode "Ukjent"). Den kaller `finnEllerOpprettSak` inne i API-forespørselen og lagrer `opprettetNy = true`. Løsningen kan også sette `journalposttittel`, `avsenderMottaker` og `dokumenter` (titler), som SETT_FAGSAK skriver til Joark. `Saksvurdering`s `init` validerer kombinasjonen.

## SETT_FAGSAK (`SettFagsakSteg`)

| Situasjon | Resultat |
| --- | --- |
| ugyldig eller `JOURNALFOERT` | `Fullført` |
| tema-vurdering UKJENT | `Fullført`, ingenting endres i Joark |
| tema ikke AAP/OPP | dokarkiv `endreTema` (bare hvis Joark fortsatt sier AAP), `Nav-User-Id` = den som løste AVKLAR_TEMA |
| generell sak | dokarkiv oppdater: generell sak, tema, tittel, avsender, dokumenttitler |
| fagsak | dokarkiv oppdater: Kelvin-sak (fagsystem KELVIN), bruker = aktiv ident, tema AAP, tittel, avsender, dokumenttitler |

`Nav-User-Id` = den som løste AVKLAR_SAK. Avsender/mottaker fra vurderingen ignoreres for digitale kanaler. Uten den bruker `JournalføringService` brukeren som avsender når SAF ikke har noen avsender-id. En orgnr-avsender slås opp i EREG og blir `UTL_ORG` (navn kreves) hvis den ikke finnes.

## ENDELIG_JOURNALFØRING (`JournalføringSteg`)

ugyldig, allerede `JOURNALFOERT`, eller tema ikke AAP/OPP → `Fullført`. Ellers dokarkiv `ferdigstill` med journalførende enhet `9999` (maskinell), `Nav-User-Id` = den som løste AVKLAR_SAK.

## VIDERESEND (`VideresendSteg`)

| Situasjon | Resultat |
| --- | --- |
| ugyldig | `Fullført` |
| `JOURNALFOERT` på et annet fagsystem enn KELVIN | `Fullført` |
| tema-vurdering `skalTilAap == false`, eller generell sak | `Fullført` |
| ellers | opprett en `DokumentHåndtering`-behandling, kopier vurderinger med `GrunnlagKopierer`, legg prosesseringen i kø |

## Temascenarioer

| Scenario | Hva som skjer |
| --- | --- |
| Vanlig AAP-dokument | tema AAP → sak → Joark oppdater + ferdigstill → DokumentHåndtering |
| «Ikke AAP», toggle `PostmottakVelgTema` **av** | vurdering UKJENT + `VENT_PA_GOSYS`; AVKLAR_TEMA åpnes igjen og behandlingen venter ved AVKLAR_TEMA, med en Gosys-oppgave for å endre tema. Når Gosys flytter journalposten til et annet tema, kansellerer neste kjøring AVKLAR_TEMA og lukker Gosys AAP-oppgavene, **men ventingen blir stående til fristen** (verifisert). De resterende stegene fullføres så uten å røre Joark. Kommer journalposten i stedet tilbake som AAP, fjerner Kafka ventingen, og saksbehandleren svarer på AVKLAR_TEMA igjen. |
| «Ikke AAP», toggle **på** | saksbehandleren velger tema; ingen venting. SETT_FAGSAK endrer tema i Joark; UKJENT endrer ingenting; OPP journalføres på generell sak. |
| Journalposten kommer tilbake til AAP etter en ferdig behandling | AAP-retur i Kafka-handleren oppretter en ny Journalføring-behandling (se `mottak.md`). |
| Journalført andre steder mens den venter | Stegene kortslutter. På en Kelvin-sak lagrer AVKLAR_SAK det saksnummeret, og VIDERESEND oppretter fortsatt DokumentHåndtering. På et annet fagsystem avsluttes behandlingen bare. |
| Journalposten blir ugyldig | Alle steg returnerer `Fullført`, åpent AVKLAR_TEMA kanselleres, og behandlingen avsluttes. |
