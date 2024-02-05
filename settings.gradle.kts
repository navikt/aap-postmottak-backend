plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "behandlingflyt"

include(
    "app",
    "sakogbehandling",
    "faktagrunnlag",
    "verdityper",
    "tidslinje",
    "dbflyway",
    "dbconnect",
    "dbtest",
    "dbtestdata",
    "motor",
    "httpklient"
)
