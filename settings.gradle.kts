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

val githubPassword: String? by settings

dependencyResolutionManagement {
    // Felles for alle gradle prosjekter i repoet
    @Suppress("UnstableApiUsage")
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
        mavenCentral()
        maven("https://packages.confluent.io/maven/")
        mavenLocal()
    }
}
