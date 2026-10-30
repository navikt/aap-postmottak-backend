/*
Kjøres av Flyway før hver migrering mot en testdatabase (filen ligger i lib-test, som bare er på test-classpath).
Lagres ikke i flyway_schema_history, så den påvirker ikke checksummer.

Testdatabasene er engangs: de lever like lenge som test-JVM-en. Da trenger vi ikke at Postgres venter på at
data er skrevet til disk ved hver commit.
Kjører også når TestApp/ApiTest migrerer mot compose-Postgres på 5441; begge brukerne er superbruker.
ALTER SYSTEM kan ikke kjøres i en transaksjon, så Flyway kjører denne filen uten.
Innstillingene tas i bruk av beforeMigrate__2_testdb_last_konfig.sql.
*/
ALTER SYSTEM SET fsync = off;
ALTER SYSTEM SET synchronous_commit = off;
ALTER SYSTEM SET full_page_writes = off;
