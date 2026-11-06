---
name: ny-jobb
description: Oppskrift for ny bakgrunnsjobb (JobbUtfører) i Motor, periodisk eller hendelsesdrevet, med registrering og tester. Bruk når noe skal skje asynkront, på cron, mot Arena eller Gosys, eller for mange journalposter.
---
# Ny jobb (JobbUtfører)

Les `.github/instructions/jobber.instructions.md`. Avklar: periodisk (cron) eller lagt i kø av kode? Hva skjer hvis
jobben kjører to ganger, eller feiler etter at et eksternt kall har lyktes?

Reelt eksempel: `f868126ad` (#339, `JoarkAvstemmerJobbUtfører` med cron, ny gateway og klient, 11 filer).
Enklere mal i koden: `ProsesserBehandlingJobbUtfører`.

1. `flyt/…/<område>/XxxJobbUtfører.kt`: `class XxxJobbUtfører(avhengigheter) : JobbUtfører { override fun utfør(input) }`
   med `companion object : ProvidersJobbSpesifikasjon { konstruer(repositoryProvider, gatewayProvider); type = "…";
   navn; beskrivelse }`, evt. `retries` og `cron = CronExpression.create("0 0 3 * * *")`. Arena-jobber arver
   `ArenaJobbutførerBase` og implementerer `utførArena`.
2. `type` er en stabil, unik streng (lagres i jobbtabellen). Følg mønsteret i området (`flyt.…`, `batch.…`, `arena.…`).
3. Registrer i `flyt/…/postmottak/prosessering/ProsesseringsJobber.kt`.
4. Parametre: legg `medXxx`/`getXxx` i `prosessering/JobbInputExtensions.kt`; legg i kø med
   `flytJobbRepository.leggTil(JobbInput(XxxJobbUtfører).medJournalpostId(…))`, der `FlytJobbRepository` kommer fra
   `repositoryProvider.provide()` (mal: `VideresendSteg`).
5. Idempotens: sjekk lagret tilstand først. Tar jobben en behandling: `TaSkriveLåsRepository.lås` først,
   `verifiserSkrivelås` til slutt.
6. Ny gateway: skillen `ny-klient`.
7. Tester: `XxxJobbUtførerTest` i `flyt/src/test` med mockk (mal: `VurderRelevantDokumentForAAPJobbUtførerTest`), med
   `utfør` to ganger og feilgrenen. `:arkitektur:test` sjekker registreringen. `Flyttest` (Docker) for hele løkka.
