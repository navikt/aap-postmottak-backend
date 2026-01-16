import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("aap.conventions")
    alias(libs.plugins.ktor)
}

application {
    mainClass.set("no.nav.aap.postmottak.AppKt")
}

kotlin.sourceSets["test"].kotlin.srcDirs("src/systemtest/kotlin")

tasks {
    val projectProps by registering(WriteProperties::class) {
        destinationFile = layout.buildDirectory.file("version.properties")
        // Define property.
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
}

fun runCommand(command: String): String {
    val execResult = providers.exec {
        this.workingDir = project.projectDir
        commandLine(command.split("\\s".toRegex()))
    }.standardOutput.asText

    return execResult.get()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}

dependencies {
    implementation(libs.ktorSerializationJackson)

    implementation(libs.ktorOpenApiGen)

    implementation(libs.jacksonDatatypeJsr310)
    implementation(libs.jacksonDatabind)
    implementation(libs.micrometerRegistryPrometheus)
    implementation(libs.logbackClassic)
    implementation(libs.logstashLogbackEncoder)

    implementation(project(":klienter"))
    implementation(project(":repository"))
    implementation(project(":api"))
    implementation(project(":flyt"))

    implementation(libs.tilgangPlugin)
    implementation(libs.behandlingsflytKontrakt)
    implementation(libs.tilgangKontrakt)
    implementation(libs.httpklient)
    implementation(libs.infrastructure)
    implementation(libs.dbconnect)
    implementation(libs.dbmigrering)
    implementation(libs.motor)
    implementation(libs.motorApi)
    implementation(libs.server)
    implementation(libs.hikariCp)
    implementation(libs.flywayDatabasePostgresql)
    runtimeOnly(libs.postgresql)
    // Auditlogging
    runtimeOnly(libs.logbackSyslog)

    // Kafka
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation(libs.avro)

    implementation(libs.kafkaStreamsAvroSerde)
    implementation(libs.teamdokumenthandteringAvroSchemas)

    testImplementation(libs.kafkaStreamsTestUtils)

    testImplementation(libs.dbtest)
    testImplementation(project(":lib-test"))
    testImplementation(libs.bundles.junit)
    testImplementation(libs.testcontainersPostgres)
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}
