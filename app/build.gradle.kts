import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.ByteArrayOutputStream

val ktorVersion = "3.0.0"
val aapLibVersion = "5.0.23"
val komponenterVersjon = "1.0.50"
val kafkaVersion = "3.7.0"
val tilgangVersjon = "0.0.29"


plugins {
    id("postmottak.conventions")
    id("io.ktor.plugin") version "3.0.0"
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
    val byteOut = ByteArrayOutputStream()
    project.exec {
        commandLine = command.split("\\s".toRegex())
        standardOutput = byteOut
    }
    return String(byteOut.toByteArray()).trim()
}

fun getCheckedOutGitCommitHash(): String {
    if (System.getenv("GITHUB_ACTIONS") == "true") {
        return System.getenv("GITHUB_SHA")
    }
    return runCommand("git rev-parse --verify HEAD")
}

dependencies {
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-server-double-receive-jvm:2.3.5")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("no.nav:ktor-openapi-generator:1.0.46")

    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.2")
    implementation("io.micrometer:micrometer-registry-prometheus:1.13.4")
    implementation("ch.qos.logback:logback-classic:1.5.8")
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    implementation(project(":sakogbehandling"))
    implementation(project(":verdityper"))
    implementation(project(":dbflyway"))
    implementation(project(":klienter"))
    implementation(project(":fordeler"))

    implementation("no.nav.aap.tilgang:plugin:$tilgangVersjon")
    implementation("no.nav.aap.tilgang:api-kontrakt:$tilgangVersjon")
    implementation("no.nav.aap.kelvin:httpklient:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:infrastructure:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbconnect:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:dbmigrering:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:motor-api:$komponenterVersjon")
    implementation("no.nav.aap.kelvin:server:$komponenterVersjon")
    implementation(project(":faktagrunnlag"))
    implementation("com.zaxxer:HikariCP:6.0.0")
    implementation("org.flywaydb:flyway-database-postgresql:10.17.3")
    runtimeOnly("org.postgresql:postgresql:42.7.3")

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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("org.testcontainers:postgresql:1.20.1")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation(kotlin("test"))
}
