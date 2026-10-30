---
name: nytt-steg
description: Oppskrift for å legge til et nytt steg i en av de tre flytene (StegType, Steg-klasse, flytdefinisjon, evt. visning og repository, tester). Bruk når noen ber om et nytt steg eller å «legge noe inn i flyten».
---
# Nytt steg i en flyt

Før du starter: les `.github/instructions/steg.instructions.md`. Et nytt steg endrer publisert kontrakt (`StegType`)
og flytrekkefølgen for åpne behandlinger. Avklar med mennesket:
1. Hvilken flyt (`Fordelingflyt`, `Journalføringsflyt`, `Dokumentflyt`) og **hvor** i rekkefølgen?
2. Kan steget stoppe flyten (avklaringsbehov)? Da trengs også skillen `nytt-avklaringsbehov`.
3. Skal steget vises for saksbehandler (`StegGruppe` med `skalVises`)? Trenger det lagret vurdering?
4. Hvilke brevkoder eller journalposttyper gjelder det, og hva skjer med behandlinger som allerede står i flyten?

Reelle eksempler: `c37633d85` (minste mulige steg: `AvsluttBehandlingSteg`, 5 filer) og `999869785` (#733, ny
fordelingsflyt med to steg, repository og toggle, 21 filer). Stiene var andre den gang; bruk dagens.

1. `kontrakt/…/steg/StegType.kt`: ny verdi med `gruppe` og `status` (+ ny verdi i `StegGruppe.kt` hvis ny gruppe).
   Kontraktendring: si fra, og nevn aap-oppgave/aap-statistikk i PR-en.
2. `flyt/…/postmottak/forretningsflyt/steg/<flyt>/XxxSteg.kt`: `class XxxSteg(avhengigheter) : BehandlingSteg` med
   `companion object : FlytSteg { konstruer(repositoryProvider, gatewayProvider); type() = StegType.XXX }`. Start
   `utfør` med «vurdering finnes → `Fullført`» (mal: `AvklarFordelingSteg`), eller bruk `AvklaringsbehovService` for
   behov som kan reises og avbrytes (mal: `AvklarTemaSteg`).
3. `forretningsflyt/behandlingstyper/<Flyt>.kt`: `.medSteg(steg = XxxSteg)` på riktig plass, evt.
   `informasjonskrav = listOf(…)`.
4. Ny vurdering som lagres: grensesnitt i `flyt` ved siden av modellen, `repository/…/XxxRepositoryImpl.kt` med
   `kopier()`, `PostgresRepositoryRegistry.kt`, migrasjon `V1.<neste>__….sql` (se `repository.instructions.md`).
5. Ny synlig gruppe som ikke er obligatorisk: `flyt/…/flyt/flate/visning/XxxVisningUtleder.kt`.
6. Tester: `XxxStegTest` i `flyt/src/test/…/forretningsflyt/steg/<flyt>/` med mockk (kjør `utfør` to ganger;
   samme resultat, ingen nye behov), scenario i `Flyttest` hvis flyten endres, `XxxRepositoryImplTest` ved nytt repository.
7. Kjør `./gradlew :arkitektur:test` (steget må stå i en flyt, repositoryet må være registrert) og skillen `verifiser`.
