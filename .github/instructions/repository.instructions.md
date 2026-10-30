---
applyTo: "repository/src/main/**"
description: "Repositories og Flyway: håndskrevet SQL, Factory og registrering, aktiv-flagg, kopier, enum som tekst, migrasjoner som tåler rullende deploy"
---
# Regler for repositories og migrasjoner

## Repositories
- Håndskrevet SQL via `DBConnection` (`queryFirstOrNull`/`queryList`/`execute`/`executeReturnKey` med `setParams` og
  `setRowMapper`). Ingen ORM. Parametre er posisjonelle: setter du inn en kolonne midt i en INSERT, flytt alle indekser etter.
- Ny impl: `class XxxRepositoryImpl(private val connection: DBConnection) : XxxRepository` med
  `companion object : Factory<XxxRepositoryImpl>`, og registrer i `PostgresRepositoryRegistry.kt` (kompilerer uten,
  feiler i runtime; `:arkitektur:test` sier fra). Grensesnittet (`: Repository` fra `no.nav.aap.lookup.repository`)
  ligger i `flyt` ved siden av modellen.
- Grunnlag: vurderingstabell + grunnlagstabell med `AKTIV` per `behandling_id`. Skriv med «`UPDATE … SET AKTIV = FALSE`
  + insert ny rad», les med `AND AKTIV`. Annen `UPDATE` mot tabeller med `aktiv` er forbudt (`PersistensTest`).
  Mal: `AvklarTemaRepositoryImpl`. Journalposter lagres som nye snapshot-rader og leses med
  `ORDER BY OPPRETTET_TID DESC LIMIT 1`.
- Grunnlag som skal følge journalposten til neste behandling, implementerer `kopier(fra, til)` (fem repositories gjør
  det i dag). `Repository.slett(behandlingId)` (GDPR) er ikke implementert av noe repository; ikke lat som det finnes.
- Enumer lagres med `setEnumName`/`getEnum` (navnet er dataen). Ny verdi er greit, omdøping krever datamigrasjon og
  `@Deprecated`-verdi for gamle rader.
- `INNKOMMENDE_JOURNALPOST.bruker_id` og `PERSON_IDENT` har fødselsnummer i klartekst. Ikke logg rader eller
  resultater fra disse tabellene.

## Migrasjoner (`src/main/resources/flyway/`)
- Navn: `V1.<neste nummer>__beskrivelse_med_understrek.sql` (høyeste nå: `V1.77`). Sjekk nummeret rett før PR; to
  PR-er med samme nummer kolliderer. Aldri rediger en migrasjon som er merget.
- Migrasjonen kjører ved oppstart av første nye pod mens gamle poder (opptil 4) kjører gammel kode mot nytt skjema:
  nye kolonner nullable eller `NOT NULL DEFAULT …`; `DROP`/`RENAME`/`ALTER TYPE` bare i en egen PR etter at koden
  som brukte dem er i prod.
- Migrasjon, repository-endring og test hører i samme PR. Halvparten av migrasjons-commitene har hatt bare SQL-fila,
  og #898 → #900 viser hva det koster.
- Test: `XxxRepositoryImplTest` i `repository/src/test` med `@Tag("db")` og `@AutoClose private val dataSource =
  TestDataSource()` (mal: `AvklarFordelingRepositoryImplTest`); dekk lagre/hent, deaktivering og `kopier`. Alle
  migrasjoner kjøres mot tom Postgres i hver DB-test. Krever Docker: agenten kompilerer, mennesket eller CI kjører.
