---
applyTo: "api/src/main/kotlin/**,app/src/main/kotlin/**"
description: "Ktor-ruter og DTO-er i api og app: autorisasjon, transaksjon per kall, skrivelås ved endring, OpenAPI som frontend-kontrakt"
---
# Regler for api og app

- Ingen forretningslogikk i rutefiler. Ruten åpner transaksjon (`dataSource.transaction { connection -> … }`, med
  `readOnly = true` for GET), lager `repositoryRegistry.provider(connection)`, kaller domenet, mapper til DTO.
- Autorisasjon via tilgang-pluginen: `authorizedGet`/`authorizedPost` med riktig `Operasjon` og en resolver for
  journalpost eller behandling. Ingen rute uten. Unntakene er merket `@Suppress("UnauthorizedGet")`; ikke lag flere.
- Ruter som endrer en behandling tar skrivelåsen (`TaSkriveLåsRepository.lås`/`verifiserSkrivelås`) og validerer
  versjonen med `BehandlingTilstandValidator` (gir HTTP 409 ved utdatert versjon eller jobb i kø). Mal:
  `AvklaringsbehovApi.kt`.
- Ny rute: `fun NormalOpenAPIRoute.xxxApi(dataSource, repositoryRegistry, gatewayProvider)` i riktig pakke, monter i
  `App.kt`. Endret request/respons endrer `openapi.json` (`./gradlew :app:genererOpenApiJson`, krever Docker fordi den
  starter Postgres; uten Docker: si at fila må regenereres), som frontendene genererer typer fra; nevn det i PR-beskrivelsen.
- Auditlogg: `DefaultAuditLogConfig.fraBehandlingPathParam`/`fraJournalpostPathParam` slår opp identen og logger
  til auditloggen (brukt i `auditlog/AuditlogApi.kt`). Ny rute som viser personopplysninger eller dokumenter: spør
  mennesket om den skal auditlogges.
- Hold `DBConnection`-SQL, `*RepositoryImpl` og klienter ute av `api`; bruk grensesnittene fra `flyt`
  (`:arkitektur:test` sjekker importene).
- `api/…/test/` er testendepunkter som bevisst ligger i `main` (skrudd av i prod). Ikke slett, ikke utvid med prodlogikk.
- `app/…/App.kt` er komposisjonsroten (Flyway, Motor, Kafka-strømmen, API-moduler). Endringer der påvirker oppstart i
  prod; kjør `:app:test --tests "*ApiTest"` (krever Docker) eller be mennesket gjøre det.
- `api` har ingen tester i dag. Ny logikk som trengs av en rute, hører hjemme i `flyt` med test der.
