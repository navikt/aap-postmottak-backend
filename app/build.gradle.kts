import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(kelvinLibs.plugins.ktor)
}

application {
    mainClass.set("no.nav.aap.postmottak.AppKt")
}

kotlin.sourceSets["test"].kotlin.srcDirs("src/systemtest/kotlin")

tasks {
    val projectProps = register<WriteProperties>("projectProps") {
        description = "Project props."
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property. Lazy Provider, unngår å kjøre git-kommandoen under konfigurasjonsfasen.
        property("project.version", getCheckedOutGitCommitHash())
    }

    processResources {
        // Depend on output of the task to create properties,
        // so the properties file will be part of the Java resources.
        from(projectProps)
    }

    withType<ShadowJar> {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
        mergeServiceFiles()
    }

    register<JavaExec>("genererOpenApiJson") {
        group = "documentation"
        description = "Kjør generering av OpenAPI JSON-fil. Filen blir skrevet til openapi.json"
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("no.nav.aap.postmottak.GenererOpenApiJsonKt")
    }

    register<JavaExec>("runTestApp") {
        group = "application"
        description = "Kjør TestApp, med en testcontainer-postgres, lokalt på port 8070."
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("no.nav.aap.postmottak.TestAppKt")
    }

    register<JavaExec>("runTestAppMotBehandlingsflyt") {
        group = "application"
        description = "Kjør TestApp mot lokal behandlingsflyt og oppgave, tilsvarende run-konfigurasjonen 'TestApp (mot behandlingsflyt)'."
        classpath = sourceSets.test.get().runtimeClasspath
        mainClass.set("no.nav.aap.postmottak.TestAppKt")
        environment(
            "NAIS_CLUSTER_NAME" to "LOCAL",
            "DB_POSTMOTTAK_JDBC_URL" to "jdbc:postgresql://localhost:5441/postgres",
            "DB_POSTMOTTAK_USERNAME" to "postgres",
            "DB_POSTMOTTAK_PASSWORD" to "",
            "INTEGRASJON_BEHANDLINGSFLYT_URL" to "http://localhost:8080",
            "INTEGRASJON_OPPGAVE_URL" to "http://localhost:8084",
            "INTEGRASJON_POSTMOTTAK_AZP" to "c62cff74-505a-4858-ac15-16061c2e8290"
        )
    }
}

fun runCommand(command: String): Provider<String> =
    providers.exec {
        this.workingDir = project.projectDir
        commandLine(command.split("\\s".toRegex()))
    }.standardOutput.asText.map { it.trim() }

fun getCheckedOutGitCommitHash(): Provider<String> {
    val isGithubActions = providers.environmentVariable("GITHUB_ACTIONS").map { it == "true" }.orElse(false)
    val githubSha = providers.environmentVariable("GITHUB_SHA")
    return isGithubActions.flatMap { isCi -> if (isCi) githubSha else runCommand("git rev-parse --verify HEAD") }
}

dependencies {
    implementation(kelvinLibs.jackson.datatype.jsr310)
    implementation(kelvinLibs.jackson.databind)
    implementation(kelvinLibs.micrometer.prometheus)
    implementation(kelvinLibs.logback.classic)
    implementation(kelvinLibs.logstash.logback.encoder)

    implementation(project(":klienter"))
    implementation(project(":repository"))
    implementation(project(":api"))
    implementation(project(":flyt"))

    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.motor)
    implementation(libs.motor.api)
    implementation(libs.server)
    implementation(kelvinLibs.hikaricp)
    // Auditlogging
    runtimeOnly(kelvinLibs.logback.syslog)

    // Kafka
    implementation(kelvinLibs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.avro)

    implementation(libs.kafka.streams.avro.serde)
    implementation(libs.teamdokumenthandtering.avro.schemas)

    testImplementation(libs.dbtest)
    testImplementation(project(":lib-test"))
    testImplementation(kelvinLibs.bundles.junit)
    testImplementation(libs.testcontainers.postgres)
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(kelvinLibs.mockk)
    testImplementation(kotlin("test"))
    testImplementation(libs.kafka.streams.test.utils)
}
