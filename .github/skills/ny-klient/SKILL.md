---
name: ny-klient
description: Oppskrift for ny integrasjon mot et eksternt system (Gateway-grensesnitt, klient med Factory, DefaultGatewayProvider, .nais-outbound, fake i lib-test, test). Bruk når postmottak skal hente fra eller sende til et nytt system eller endepunkt.
---
# Ny klient og gateway

Les `.github/instructions/klienter.instructions.md`. Avklar: hvilket system og miljø (dev-gcp/dev-fss), M2M eller
OBO, hva skjer når systemet svarer 404 eller er nede, og inneholder svaret persondata?

Reelt eksempel: `5af654abc` (#639, `EREGKlient` for utenlandsk orgnummer, 12 filer, med toggle og `.nais`). Filene het
`.nais/dev.yaml`/`prod.yaml` den gang; i dag `.nais/dev-gcp.yaml` og `.nais/prod-gcp.yaml`.

1. `flyt/…/postmottak/gateway/XxxGateway.kt`: `interface XxxGateway : Gateway` og DTO-ene domenet trenger.
2. `klienter/…/klient/<system>/XxxKlient.kt`: `RestClient.withDefaultResponseHandler(config, tokenProvider, prometheus)`,
   `requiredConfigForKey("INTEGRASJON_XXX_URL")`/`"_SCOPE"`, `companion object : Factory<XxxKlient>` med `by lazy`.
   Mal: `ereg/EREGKlient.kt`.
3. Registrer i `klienter/…/klient/DefaultGatewayProvider.kt`.
4. `.nais/dev-gcp.yaml` og `.nais/prod-gcp.yaml`: `INTEGRASJON_XXX_URL`/`_SCOPE` og `accessPolicy.outbound`.
   Si fra at dette må stemme med det andre teamets inbound-regel.
5. `lib-test/…/FakeServers.kt`: ny fake-server (helst i `fakes/Xxx.kt`) som setter `INTEGRASJON_XXX_URL`, med
   syntetiske data fra `FakeKonstanter`/`TestPersoner`.
6. Ny toggle hvis integrasjonen skal kunne slås av: `PostmottakFeature` + begge `when` i `FakeUnleash.kt`.
7. Tester: `klienter/src/test/…/XxxKlientTest.kt` med `@Fakes` (mal: `NomKlientTest`), både glade svar og feilsvar.
   `:arkitektur:test` sjekker registreringen. Logg aldri respons-objekter med persondata.
