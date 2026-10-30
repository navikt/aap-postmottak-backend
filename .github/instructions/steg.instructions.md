---
applyTo: "flyt/src/main/kotlin/no/nav/aap/postmottak/forretningsflyt/**,**/*Steg.kt"
description: "Steg og flytdefinisjoner: idempotent utfør, FantAvklaringsbehov eller AvklaringsbehovService, StegType i kontrakt, stegrekkefølge"
---
# Regler for steg og flytdefinisjoner

- `utfør(kontekst)` kjøres mange ganger for samme behandling (hver `ProsesserBehandling`-jobb, hver tilbakeføring).
  To husidiomer: «vurdering finnes → `Fullført`» som første sjekk (`AvklarFordelingSteg`, `DigitaliserDokumentSteg`),
  eller avstemming av behovet via `AvklaringsbehovService` (`AvklarTemaSteg`). Skriv bare hvis noe er endret; aldri
  «insert uten å sjekke». `OverleverTilFagsystemSteg` sender til behandlingsflyt ved hver kjøring; ikke kopier det.
- Returverdier: `Fullført` eller `FantAvklaringsbehov(Definisjon.X)`. Behov som kan reises og avbrytes igjen (som
  `AVKLAR_TEMA`) går via `AvklaringsbehovService.oppdaterAvklaringsbehov(definisjon, vedtakBehøverVurdering,
  erTilstrekkeligVurdert, kontekst)`, ikke ved å returnere behovet.
- Nytt steg: `class XxxSteg(avhengigheter) : BehandlingSteg` med `companion object : FlytSteg { konstruer(repositoryProvider,
  gatewayProvider); type() }`, ny verdi i `kontrakt/…/steg/StegType.kt` med `gruppe` og `status` (kontraktendring!),
  og `.medSteg(steg = XxxSteg)` i riktig flyt i `forretningsflyt/behandlingstyper/`. `:arkitektur:test` feiler hvis
  steget ikke er med i noen flyt.
- Informasjonskrav (`JournalpostInformasjonskrav`, `SaksnummerInfoKrav`) knyttes til steget med
  `.medSteg(steg = …, informasjonskrav = listOf(…))` og kjører før `utfør`. `ENDRET` tilbakefører flyten til steget,
  så et krav må bare svare `ENDRET` når innholdet faktisk er endret. Nytt krav: `companion object :
  Informasjonskravkonstruktør`, og `:arkitektur:test` sjekker at det står i en flyt.
- Ny `StegGruppe` med `skalVises = true` og `obligatoriskVisning = false` trenger en `StegGruppeVisningUtleder` i
  `flyt/flate/visning/` (sealed, finnes via refleksjon); uten den kaster `DynamiskStegGruppeVisningService`.
- Stegrekkefølgen og `StegType`-navnene ligger i `STEG_HISTORIKK` for åpne behandlinger. Flytt, fjern eller gi nytt
  navn til steg bare etter at mennesket har bekreftet; tidligere omdøpinger krevde datamigrasjon (`V1.10`, `V1.37`).
- Nest siste steg i `Fordeling` og `Journalføring` oppretter neste behandling: `FordelingVideresendSteg` (bare når
  fordelingen ble KELVIN) og `VideresendSteg`. Siste steg i alle flyter er `AvsluttBehandlingSteg`.
  `VideresendSteg` kopierer grunnlaget til `DokumentHåndtering` med `GrunnlagKopierer`; et nytt grunnlag som skal
  følge med, må ha `kopier()` i repositoryet.
- Kall mot Joark, Gosys eller Arena i et steg må tåle at steget kjøres igjen etter at kallet har lyktes. Sjekk hva
  som er lagret eller hva mottakeren svarer på et gjentatt kall før du legger inn et nytt.
- Test: `XxxStegTest` i `flyt/src/test` med `mockk(relaxed = true)` for repositories og gateways (mal:
  `AvklarTemaStegTest`, som også viser `InMemoryAvklaringsbehovRepository`). Dekk «vurdering finnes»-grenen og hver
  `when`-gren. Endres flyten, legg til eller oppdater et scenario i `Flyttest` (krever Docker, kjøres av menneske/CI).
