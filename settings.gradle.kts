pluginManagement {
    includeBuild("build-logic")
}

rootProject.name = "postmottak"

include(
    "api",
    "app",
    "lib-test",
    "repository",
    "kontrakt",
    "flyt",
    "klienter"
)

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    versionCatalogs {
        create("kelvinLibs") {
            from("no.nav.aap.kelvin:version-catalog:2.0.163")
        }
    }
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release") {
            // Denne mirroren har kun no.nav-artefakter; innholdsfiltrering unngår at Gradle
            // søker her for hver avhengighet fra andre repoer.
            content { includeGroupByRegex("no\\.nav\\..*") }
        }
        mavenCentral()
        maven("https://packages.confluent.io/maven/") {
            // io.confluent-artefakter, samt Confluent sine egne builds av Kafka (versjoner med "-ce"-suffiks).
            content {
                includeGroup("io.confluent")
                includeGroup("org.apache.kafka")
            }
        }
        mavenLocal()
    }
}
