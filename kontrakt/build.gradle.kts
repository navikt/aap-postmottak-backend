plugins {
    id("postmottak.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.19.2")
    api("no.nav:ktor-openapi-generator:1.0.119")
    compileOnly(libs.tilgangKontrakt)

    testImplementation(libs.bundles.junit)
}

group = "no.nav.aap.postmottak"

apply(plugin = "maven-publish")
apply(plugin = "java-library")

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.name
            version = project.findProperty("version")?.toString() ?: "0.0.0"
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
