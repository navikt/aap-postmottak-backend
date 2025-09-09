plugins {
    id("postmottak.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.20")
    api("no.nav:ktor-openapi-generator:1.0.123")
    compileOnly(libs.tilgangKontrakt)

    testImplementation(libs.bundles.junit)
}

apply(plugin = "java-library")

java {
    withSourcesJar()
}

apply(plugin = "maven-publish")

group = "no.nav.aap.postmottak"
version = findProperty("version")?.toString() ?: "0.0.0"

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/navikt/aap-postmottak-backend")
            credentials {
                username = "x-access-token"
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
