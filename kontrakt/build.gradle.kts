import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode

plugins {
    id("postmottak.conventions")
    `maven-publish`
    `java-library`
}

dependencies {
    api(libs.jacksonAnnotations)
    api(libs.ktorOpenApiGen)
    compileOnly(libs.tilgangKontrakt)

    testImplementation(libs.bundles.junit)
}

apply(plugin = "java-library")

java {
    withSourcesJar()
}

kotlin {
    explicitApi = ExplicitApiMode.Warning
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
