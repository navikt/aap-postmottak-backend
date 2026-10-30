---
name: ny-regel
description: Oppskrift for ny fordelingsregel som avgjør om en journalpost går til Kelvin eller Arena (Regel, InputGenerator, RegelFactory, miljøConfig, tester). Bruk når noen vil endre hvem som fordeles til Kelvin.
---
# Ny fordelingsregel

Les `.github/instructions/regler.instructions.md`. Avklar med mennesket før du starter:
1. Skal regelen være et krav for Kelvin (standard: alle aktive regler må være sanne), eller bare måles/overstyre?
2. Aktiv i dev, prod eller begge (`miljøConfig(prod = …, dev = …)`)? Husmønsteret er dev først.
3. Hvilke data trengs, og fra hvilken gateway? Finnes data allerede i `RegelInput` (journalpostId, person, brevkode,
   mottattDato)?

Reelle eksempler: `24978f5a6` (`ErIkkeAnkeRegel`, 3 filer: regel, `FordelerRegelService`, `RegelResultatTest`) og
`f49999c03` (#387, `ManueltOverstyrtTilArenaRegel` med repository og migrasjon, 8 filer).

1. `flyt/…/fordeler/regler/XxxRegel.kt`: `class XxxRegel : Regel<XxxInput>` (`vurder`, `regelNavn() = this::class.simpleName!!`),
   `companion object : RegelFactory<XxxInput> { override val erAktiv = miljøConfig(…); medDataInnhenting(…) }`,
   `class XxxInputGenerator : InputGenerator<XxxInput>` og `data class XxxInput`. Mal: `Aldersregel.kt`.
   Klassen må ligge i denne pakken (sealed); da finner `FordelerRegelService` den selv.
2. Klassenavnet lagres i `REGEL_EVALUERING.regel_navn`. Velg navnet med omhu; det endres ikke etterpå.
3. Bare hvis regelen ikke skal være et vanlig krav: endre `Regelresultat.skalTilKelvin()` i `FordelerRegelService.kt`
   (som for `KelvinSakRegel`, `ArenaSakRegel`, `ManueltOverstyrtTilArenaRegel`). Det er en forretningsbeslutning.
4. Nye data: gateway-kall i inputgeneratoren (se skillen `ny-klient`) eller nytt repository (+ migrasjon og registrering).
5. Tester: `flyt/src/test/…/fordeler/regler/XxxRegelTest.kt` for `vurder` og inputgeneratoren (mockk av gateway), og
   en sak i `RegelResultatTest` som viser hvordan regelen påvirker `skalTilKelvin()`.
6. Kjør skillen `verifiser`. Nevn i PR-en hvilke miljøer regelen er aktiv i, og hva som skjer med journalposter som
   allerede er evaluert (resultatet er cachet i `innkommende_journalpost`).
