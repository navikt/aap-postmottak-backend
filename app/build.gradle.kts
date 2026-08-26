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
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.databind)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.logback.classic)
    implementation(libs.logstash.logback.encoder)

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
    implementation(libs.hikari.cp)
    // Auditlogging
    runtimeOnly(libs.logback.syslog)

    // Kafka
    implementation(libs.kafka.clients)
    implementation(libs.kafka.streams)
    implementation(libs.avro)

    implementation(libs.kafka.streams.avro.serde)
    implementation(libs.teamdokumenthandtering.avro.schemas)

    testImplementation(libs.dbtest)
    testImplementation(project(":lib-test"))
    testImplementation(libs.bundles.junit)
    testImplementation(libs.testcontainers.postgres)
    constraints {
        implementation("org.apache.commons:commons-compress:1.28.0") {
            because("https://github.com/advisories/GHSA-4g9r-vxhx-9pgx")
        }
    }
    testImplementation(libs.mockk)
    testImplementation(kotlin("test"))
}
