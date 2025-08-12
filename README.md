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