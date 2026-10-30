// Felles kode for alle build.gradle.kts filer som laster inn denne conventions pluginen

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("dev.detekt")
}

detekt {
    config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
    buildUponDefaultConfig = false
}

tasks.withType(dev.detekt.gradle.Detekt::class.java).configureEach {
    reports {
        html.required.set(true)
        checkstyle.required.set(false)
        sarif.required.set(true)
        markdown.required.set(false)
    }
}

tasks.named("check").configure {
    setDependsOn(dependsOn.filterNot {
        it.toString().contains("detekt", ignoreCase = true)
    })
}

group = "no.nav.aap"
version = project.findProperty("version")?.toString() ?: "0.0.0"

// https://docs.gradle.org/8.12.1/userguide/jvm_test_suite_plugin.html
testing {
    suites {
        @Suppress("UnstableApiUsage", "unused") val test = getByName<JvmTestSuite>("test") {
            useJUnitJupiter()
        }
    }
}

/**
 * Tagger på testklasser som trenger Docker (Testcontainers). Klasser uten disse taggene er kjernetester.
 * - db: Postgres via TestDataSource eller postgreSQLContainer()
 * - kafka: Kafka-container (bare Flyttest)
 */
val dockerTagger = arrayOf("db", "kafka")

/**
 * `./gradlew coreTest` (eller `:flyt:coreTest`) kjører `test` uten testene som trenger Docker, og er ment
 * å kjøres etter hver endring. Den er et alias og ikke en egen Test-task, slik at IntelliJ bare ser `test` og ikke
 * spør hvilken task som skal kjøres når man kjører en enkelt test.
 */
val kjørerCoreTest = gradle.startParameter.taskNames.any { it.substringAfterLast(':') == "coreTest" }
if (kjørerCoreTest && gradle.startParameter.taskNames.any { it.substringAfterLast(':') in setOf("test", "check", "build") }) {
    throw GradleException("coreTest gjør om test til bare kjernetester, og kan ikke kjøres sammen med test, check eller build")
}

tasks {
    test {
        useJUnitPlatform()
        // Overstyr med -PtestForks=N, f.eks. når bare én modul testes på en maskin med mange kjerner
        maxParallelForks = providers.gradleProperty("testForks").orNull?.toInt() ?: (Runtime.getRuntime().availableProcessors() / 2)
        testLogging {
            events("passed", "skipped", "failed")
        }
        if (kjørerCoreTest) {
            useJUnitPlatform {
                excludeTags(*dockerTagger)
            }
            // Får en test uten tag til å feile med en forklaring i stedet for å starte en container
            environment("TESTCONTAINERS_IMAGE_SUBSTITUTOR", "no.nav.aap.postmottak.test.DockerIkkeTillattIKjernetester")
        }
    }

    register("coreTest") {
        description = "Kjører testene som ikke trenger Docker (alt uten taggene ${dockerTagger.joinToString()})."
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(test)
    }

    (findByName("jar") as? Jar)?.apply {
        // Bruk et unikt navn for jar-filen til hver submodul, for å unngå navnekollisjoner i multi-modul prosjekt,
        // gjennom at vi ikke bruker samme navn, feks. "kontrakt.jar" og "api.jar", i flere moduler.
        // Dette unngår feil av typen "Entry <name>.jar is a duplicate but no duplicate handling strategy has been set"
        // Alternativet er å unngå å bruke det eksakt samme navnet på submoduler fra forskjellige moduler,
        // som feks "kontrakt".
        archiveBaseName.set("${rootProject.name}-${project.name}")
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4)

        // Bruk et unikt navn for <submodule>.kotlin_module for hver Gradle-submodul, for å unngå navnekollisjoner i
        // multi-modul prosjekt, hvor vi inkluderer flere av våre kotlin-moduler i samme jar-fil eller
        // på samme runtime classpath. Kroneksempelet er "kontrakt.kotlin_module" fra både behandlingsflyt, brev,
        // meldekort og andre steder.
        // Dette gjør at vi kan beholde informasjonen for hver kotlin_module, og kotlin-reflect og andre verktøy
        // fungerer som forventet. Alternativet er å unngå å bruke det eksakt samme navnet på submoduler fra
        // forskjellige moduler, som feks "kontrakt".
        moduleName.set("${rootProject.name}-${project.name}")
    }
}

// Pass på at når vi kaller JavaExec eller Test tasks så bruker vi samme språk-versjon som vi kompilerer til
val toolchainLauncher = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}
tasks.withType<Test>().configureEach {
    javaLauncher.set(toolchainLauncher)
    systemProperty("net.bytebuddy.safe", "true")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
tasks.withType<JavaExec>().configureEach { javaLauncher.set(toolchainLauncher) }


kotlin.sourceSets["main"].kotlin.srcDirs("main")
kotlin.sourceSets["test"].kotlin.srcDirs("test")
sourceSets["main"].resources.srcDirs("main")
sourceSets["test"].resources.srcDirs("test")