plugins {
    id("postmottak.conventions")
    `maven-publish`
    `java-library`
}

val tilgangVersjon = "1.0.53"

dependencies {
    api("com.fasterxml.jackson.core:jackson-annotations:2.18.3")
    api("no.nav:ktor-openapi-generator:1.0.105")
    compileOnly("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.12.2")
    testImplementation("org.assertj:assertj-core:3.27.3")
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
