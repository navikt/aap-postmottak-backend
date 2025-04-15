import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val ktorVersion = "3.1.2"
val komponenterVersjon = "1.0.218"
val kafkaVersion = "3.7.0"
val tilgangVersjon = "1.0.49"
val behandlingsflytVersjon = "0.0.214"
val junitVersjon = "5.12.2"

plugins {
    id("postmottak.conventions")
    id("io.ktor.plugin") version "3.1.2"
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
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive-jvm:3.1.2")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
    implementation("no.nav:ktor-openapi-generator:1.0.103")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.18.3")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.6")
    implementation("ch.qos.logback:logback-classic:1.5.18")
    implementation("net.logstash.logback:logstash-logback-encoder:8.1")

    implementation(project(":klienter"))
    implementation(project(":repository"))
    implementation(project(":api"))
    implementation(project(":flyt"))

    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.behandlingsflyt:kontrakt:$behandlingsflytVersjon")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-database-postgresql:11.7.0")
    runtimeOnly("org.postgresql:postgresql:42.7.5")
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

    testImplementation("no.nav.aap.kelvin:dbtest:$komponenterVersjon")
    testImplementation(project(":lib-test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersjon")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersjon")
    testImplementation("org.assertj:assertj-core:3.27.3")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    constraints {
        implementation("org.apache.commons:commons-compress:1.27.1") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation("io.mockk:mockk:1.14.0")
    testImplementation(kotlin("test"))
}
