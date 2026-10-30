---
applyTo: "**/*JobbUtfører.kt,flyt/src/main/kotlin/no/nav/aap/postmottak/prosessering/**,flyt/src/main/kotlin/no/nav/aap/fordeler/arena/**"
description: "Motor-jobber: ProvidersJobbSpesifikasjon, stabil type-streng, registrering i ProsesseringsJobber, idempotens og retries, Arena-jobbene"
---
# Regler for jobber (Motor)

- Ny jobb: `class XxxJobbUtfører(avhengigheter) : JobbUtfører { utfør(input) }` med
  `companion object : ProvidersJobbSpesifikasjon { konstruer(repositoryProvider, gatewayProvider); type; navn;
  beskrivelse }`, evt. `retries`/`cron`. Mal: `ProsesserBehandlingJobbUtfører` (enkel), `JoarkAvstemmerJobbUtfører` (cron).
- `type` er en stabil streng som lagres i jobbtabellen. Den endres aldri, og må være unik.
- Registrer i `prosessering/ProsesseringsJobber.alle()` (en `listOf`). Uten registrering kan koden fint lage
  `JobbInput(XxxJobbUtfører)`, men Motor kaster `NoSuchElementException` når jobben plukkes. `:arkitektur:test` sier fra.
- Legg i kø med `flytJobbRepository.leggTil(JobbInput(XxxJobbUtfører).medJournalpostId(…))` (`FlytJobbRepository`
  hentes med `repositoryProvider.provide()`, som i `VideresendSteg`); bruk
  utvidelsene i `JobbInputExtensions.kt` for parametre, og legg til nye der i stedet for å bruke strengnøkler direkte.
- Jobben er én transaksjon og prøves på nytt ved unntak (3 forsøk som standard, Arena-jobbene 4–6). Den må derfor
  være idempotent: sjekk lagret tilstand først (`VurderRelevantDokumentForAAPJobbUtfører` sjekker om
  `innkommende_journalpost` finnes).
- Jobber som rører en behandling tar skrivelåsen (`TaSkriveLåsRepository.lås` først, `verifiserSkrivelås` til slutt).
- Arena-jobbene arver `ArenaJobbutførerBase`, som avbryter hvis journalposten ikke lenger har status MOTTATT;
  implementer `utførArena`. Arena og Gosys er feilutsatte integrasjoner (18–26 % fiks-andel): test feilgrenene.
- Test: `XxxJobbUtførerTest` i `flyt/src/test` med mockk (mal: `VurderRelevantDokumentForAAPJobbUtførerTest`); kjør
  `utfør` to ganger for idempotens. `Flyttest` er eneste test av hele Motor-løkka og krever Docker.
