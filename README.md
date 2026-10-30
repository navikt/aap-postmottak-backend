# aap-postmottak-backend
Postmottak tar imot og håndterer journalposter på tema AAP.

### API-dokumentasjon
APIene er dokumentert med [Swagger](https://aap-postmottak-backend.intern.dev.nav.no/swagger-ui/index.html)

### Lokalt utviklingsmiljø:
Test containers benyttes for integrasjonstester med databasen så et verktøy for å kjøre Docker 
containers er nødvendig.<br>

For macOS og Linux anbefaler vi Colima. Det kan være nødvendig med et par tilpasninger:</br>
- `export TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE=$HOME/.colima/docker.sock`
- `export DOCKER_HOST=unix://$TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE`
- `export TESTCONTAINERS_RYUK_DISABLED=true`

Ved lokal kjøring mot aap-saksbehandling, så finnes det genererte test oppgaver på http://localhost:3000/postmottak/ 

### Tester

```bash
./gradlew coreTest                     # kjernetestene i alle moduler, uten Docker (~15 s)
./gradlew :flyt:coreTest               # kjernetestene i én modul (~11 s)
./gradlew test                         # alle testene, slik CI kjører dem (~1,5 min lokalt)
./gradlew :flyt:test --tests "*Flyttest"
```

Testklasser som trenger Docker (Testcontainers) er tagget, og `coreTest` hopper over dem:

- `@Tag("db")`: bruker Postgres via `TestDataSource` eller `postgreSQLContainer()`.
- `@Tag("kafka")`: bruker Kafka-containeren (bare `Flyttest`).

En ny test som bruker Docker uten tag feiler i `coreTest` med en melding om hvilken tag som mangler, så kjernetestene
forblir raske og kan kjøres uten Docker. `coreTest` er et alias for `test` med filter og kan ikke kombineres med
`test`/`check`/`build` i samme kommando.

Testdatabasene kjører uten fsync (`lib-test/src/main/resources/flyway/beforeMigrate__*.sql`), siden de kastes etter
hver kjøring. Filene kjører også når `TestApp`/`ApiTest` migrerer mot compose-Postgres på port 5441. Antall parallelle
test-JVM-er er halvparten av kjernene; overstyr med `-PtestForks=N`, f.eks. når bare én modul testes på en maskin med
mange kjerner (hver JVM med en `db`-test starter sin egen Postgres-container).

### Kjøre lokalt mot dev-gcp

Prosjektet inneholder en run config som kan kjøres av IntelliJ. Burde være synlig under "Run configurations" med navnet
`dev-gcp.run.xml`.

For at det skal kjøre lokalt må du gjøre følgende:
1. Hent secret med [aap-cli/get-secret.sh](https://github.com/navikt/aap-cli): \
   `get-secret` \
2. Kjør opp lokal database med: \
   `docker-compose up -d`
3. Om du ønsker å hente data fra dev til lokal maskin kan du bruke [dump-gcp-db.sh](https://github.com/navikt/aap-cli?tab=readme-ov-file#dump-gcp-dbsh)
4. Kjør `dev-gcp` fra IntelliJ.

Etter dette vil appen kjøre mot reelle data. Her kan du velge om du vil koble deg på gjennom autentisert frontend eller
f.eks. gyldig token med cURL e.l.

OBS: Krever at du har `EnvFile`-plugin i IntelliJ.