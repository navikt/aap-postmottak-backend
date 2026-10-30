---
applyTo: "klienter/src/main/kotlin/**,flyt/src/main/kotlin/no/nav/aap/postmottak/gateway/**,lib-test/src/main/kotlin/**"
description: "Klienter og gateways: grensesnitt i flyt, klient med Factory i klienter, registrering i DefaultGatewayProvider, .nais-outbound og fake i lib-test"
---
# Regler for klienter, gateways og fakes

- Grensesnittet `XxxGateway : Gateway` ligger i `flyt/…/postmottak/gateway/` med egne DTO-er. Domenet kjenner bare
  grensesnittet; parametertyper er aldri `*Klient`/`*Impl`.
- Klienten ligger i `klienter/…/klient/<system>/XxxKlient.kt`: `RestClient.withDefaultResponseHandler(config, tokenProvider,
  prometheus)`, URL og scope fra `requiredConfigForKey("INTEGRASJON_XXX_URL"/"_SCOPE")`, og
  `companion object : Factory<XxxKlient> { private val klient by lazy { XxxKlient() }; konstruer() = klient }`.
  Mal: `ereg/EREGKlient.kt`. Unntak: Joark-skriving ligger i `flyt/…/gateway/JournalføringService.kt` med
  `RestClient` direkte; ikke flytt den som del av en annen endring. Token: `AzureM2MTokenProvider` for maskin-til-maskin, OBO-varianter bare der
  saksbehandlerens tilgang skal gjelde (SAF-dokumenter i API-et).
- Registrer i `DefaultGatewayProvider.kt` (kompilerer uten, feiler i runtime; `:arkitektur:test` sier fra).
- Ny integrasjon krever også `.nais/dev-gcp.yaml` og `.nais/prod-gcp.yaml`: miljøvariablene og
  `accessPolicy.outbound`. Uten outbound-regel feiler kallet først i dev.
- Fake: utvid `lib-test/…/FakeServers.kt` (ktor-server på port 0 som setter `INTEGRASJON_*`-propertyene) med en fake
  i `fakes/`, eller lag en `Fake*Gateway` uten HTTP. `@Fakes` på en testklasse starter alle fake-serverne.
- Persondata: PDL, SAF og Gosys returnerer fødselsnummer, navn og dokumenter. Logg aldri respons-objekter; logg
  `journalpostId` og HTTP-status. Testdata kommer fra `TestPersoner`/`TestJournalPost`, ikke fra ekte svar.
- Feilutsatt: `klient/pdl` (25 % fiks-andel), `klient/norg`, `klient/gosysoppgave`. Test feilsvar (404, tom liste,
  `null`-felt), ikke bare det glade svaret.
- Test: `XxxKlientTest` i `klienter/src/test` med `@Fakes` (mal: `NomKlientTest`), uten Docker.
