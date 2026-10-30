---
name: nytt-avklaringsbehov
description: Oppskrift for nytt avklaringsbehov med AvklaringsbehovKode, Definisjon, Løsning, Løser og kobling i steget. Bruk når saksbehandler skal ta stilling til noe nytt, eller noen sier «nytt behov», «ny løser» eller «nytt ventepunkt».
---
# Nytt avklaringsbehov + løser

Les `.github/instructions/avklaringsbehov.instructions.md` først. Avklar med mennesket: hvilket steg løser behovet,
hvilke roller (`løsesAv`), er det et ventepunkt med frist, og er aap-tilgang og aap-oppgave klare for ny kode?

Reelle eksempler: `e270204ed` (#446, ventepunktet `VENT_PA_GOSYS` med ny kode `1342`, 13 filer) for kontrakt-delen, og
`83e9f58a4` (`AVKLAR_OVERLEVERING`, 20 filer) for løser, løsning, repository, migrasjon og stegtest. Merk: `83e9f58a4`
gjenbrukte kode `1341` fra et fjernet behov; det var før prod og er ikke lov i dag. `e270204ed` la ikke til
`const val` for `1342`; gjør det likevel (steg 1).

1. `kontrakt/…/avklaringsbehov/AvklaringsbehovKode.kt`: `public const val XXX_KODE: String = "NNNN"` **og** enum-verdien
   `` `NNNN` `` bakerst. Neste ledige firesifrede kode (i dag er `1337`–`1343` og `9001` brukt; aldri gjenbruk).
2. `kontrakt/…/avklaringsbehov/Definisjon.kt`: ny entry med `kode`, `type`, `løsesISteg`, evt. `løsesAv` og
   `defaultFrist`. Eksisterende entries røres ikke. Kontraktendring.
3. `flyt/…/avklaringsbehov/løsning/XxxLøsning.kt`: `@JsonTypeName(XXX_KODE)`, `@JsonIgnoreProperties(ignoreUnknown = true)`,
   `behovstype` med `defaultValue = XXX_KODE`, `: AvklaringsbehovLøsning`, `løs()` delegerer. Mal: `AvklarSaksnummerLøsning`.
4. `flyt/…/avklaringsbehov/løser/XxxLøser.kt`: `AvklaringsbehovsLøser<XxxLøsning>` med
   `companion object : LøserKonstruktør<XxxLøsning>` og unik `forBehov()`. Lagre vurderingen, returner `LøsningsResultat`.
5. Vurderingen: modell + repository-grensesnitt i `faktagrunnlag/`, `repository/…/XxxRepositoryImpl.kt` (aktiv-mønster,
   `kopier()`), `PostgresRepositoryRegistry.kt`, migrasjon.
6. Steget: `FantAvklaringsbehov(Definisjon.XXX)`, eller `AvklaringsbehovService.oppdaterAvklaringsbehov(…)` hvis behovet
   skal kunne avbrytes igjen. Først i `utfør`: vurdering finnes → `Fullført`.
7. Skal saksbehandler se grunnlaget: `api/…/faktagrunnlag/<område>/XxxApi.kt` + `App.kt`. `openapi.json` regenereres med `./gradlew :app:genererOpenApiJson`, som krever Docker;
   uten Docker: si at fila må regenereres.
8. Tester: `XxxLøserTest`, legg løseren i `forventedeLøsere` i `AvklaringsbehovsLøserTest`, stegtest for begge grener,
   `DefinisjonTest` dekker unike koder, `:arkitektur:test` dekker konstant mot enum.
9. Etterarbeid utenfor repoet (nevn i PR): bump `no.nav.aap.postmottak:kontrakt` i aap-tilgang og aap-oppgave før
   behovet kan reises i prod; frontend-typer i aap-saksbehandling.
