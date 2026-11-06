---
applyTo: "flyt/src/main/kotlin/no/nav/aap/fordeler/regler/**,flyt/src/main/kotlin/no/nav/aap/fordeler/FordelerRegelService.kt"
description: "Fordelingsregler Kelvin eller Arena: sealed Regel og RegelFactory, erAktiv per miljø, regelnavnet lagres i DB, skalTilKelvin-prioritet"
---
# Regler for fordelingsreglene

- En regel er tre klasser i `fordeler/regler/`: `XxxRegel : Regel<XxxInput>` med `vurder` og `regelNavn()`,
  `XxxInputGenerator : InputGenerator<XxxInput>` som henter data, og `data class XxxInput`. Regelen har
  `companion object : RegelFactory<XxxInput> { override val erAktiv = miljøConfig(prod = …, dev = …);
  medDataInnhenting(…) }`. Mal: `Aldersregel.kt`.
- `Regel`, `InputGenerator` og `RegelFactory` er `sealed`: nye regler må ligge i samme pakke og modul, og blir da
  funnet automatisk av `FordelerRegelService` via `RegelFactory::class.sealedSubclasses`. Det finnes ingen liste.
- `regelNavn()` (= klassenavnet) lagres i `REGEL_EVALUERING.regel_navn`, brukes i RUNBOOK-spørringer og i
  `Regelresultat`-oppslag. Å gi en regelklasse nytt navn bryter historikken og `skalTilKelvin()` for lagrede
  resultater. Lag en ny regel i stedet.
- `Regelresultat.skalTilKelvin()`: `ManueltOverstyrtTilArenaRegel` vinner alltid; Kelvin-sak og ikke reisestønad og
  ikke anke gir Kelvin; ellers må alle øvrige aktive regler være `true`. En ny regel blir altså automatisk et krav for
  å komme til Kelvin. Skal den bare måles, må `skalTilKelvin()` endres (som for `ArenaSakRegel`), og det er en
  forretningsbeslutning mennesket tar.
- `skalTilKelvin()` krever resultat fra `KelvinSakRegel`, `ArenaSakRegel`, `ErIkkeReisestønadRegel` og `ErIkkeAnkeRegel`
  (`requireNotNull`). Settes en av dem inaktiv i et miljø med `miljøConfig`, kaster fordelingen der.
- Ny regel slås på i dev før prod (`miljøConfig(prod = false, dev = true)`); skyggemodus → metrikk → aktivering er
  husmønsteret. Toggle i tillegg bare hvis den skal kunne slås av uten deploy.
- `InputGenerator` gjør eksterne kall (PDL, arenaoppslag, behandlingsflyt). Hent bare det regelen trenger; personen
  har `aktivIdent()`, men logg aldri identen.
- Området er feilutsatt (24–26 % fiks-andel, `ArenaHistorikkRegel` reversert flere ganger). Hver ny gren skal ha test.
- Test: egen `XxxRegelTest` for `vurder` og inputgeneratoren (mockk av gateways), og `RegelResultatTest` for
  hvordan regelen påvirker `skalTilKelvin()`. Alt kjører uten Docker; `ArenaOppgaveFlytTest` (Docker) for hele veien.
