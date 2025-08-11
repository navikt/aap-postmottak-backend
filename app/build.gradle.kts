import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val kafkaVersion = "3.7.0"

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

    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.2")
    implementation("no.nav:ktor-openapi-generator:1.0.120")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.19.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.15.2")
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
    implementation("com.zaxxer:HikariCP:7.0.1")
    implementation("org.flywaydb:flyway-database-postgresql:11.11.0")
    runtimeOnly("org.postgresql:postgresql:42.7.7")
    // Auditlogging
    runtimeOnly(group = "com.papertrailapp", name = "logback-syslog4j", version = "1.0.0")
    
    // kafka
    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("org.apache.kafka:kafka-streams:$kafkaVersion")
    implementation("org.apache.kafka:kafka_2.13:$kafkaVersion")
    implementation("org.apache.avro:avro:1.12.0")

    implementation("io.confluent:kafka-streams-avro-serde:7.7.1")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:08271806")

    testImplementation("org.apache.kafka:kafka-streams-test-utils:$kafkaVersion")

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
