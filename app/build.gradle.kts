import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("postmottak.conventions")
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

    implementation("com.fasterxml.jackson.core:jackson-databind:2.20.0")
    implementation("no.nav:ktor-openapi-generator:1.0.124")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.4")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

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
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.12.0")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
    // Auditlogging
    runtimeOnly("com.papertrailapp:logback-syslog4j:1.0.0")

    // Kafka
    implementation(libs.kafkaClients)
    implementation(libs.kafkaStreams)
    implementation("org.apache.avro:avro:1.12.0")

    implementation("io.confluent:kafka-streams-avro-serde:8.0.0")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:1.1.6")

    testImplementation(libs.kafkaStreamsTestUtils)

    testImplementation(libs.dbtest)
    testImplementation(project(":lib-test"))
    testImplementation(libs.bundles.junit)
    testImplementation("org.testcontainers:postgresql:1.21.3")
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.5")
    testImplementation(kotlin("test"))
}
