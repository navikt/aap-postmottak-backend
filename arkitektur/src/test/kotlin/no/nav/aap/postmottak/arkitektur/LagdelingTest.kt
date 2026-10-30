package no.nav.aap.postmottak.arkitektur

import no.nav.aap.postmottak.arkitektur.Kildekode.klasser
import no.nav.aap.postmottak.arkitektur.Kildekode.produksjon
import no.nav.aap.postmottak.arkitektur.Kildekode.tester
import org.junit.jupiter.api.Test

/**
 * Lagdelingen kompilatoren ikke kan håndheve. `flyt` ser `dbconnect` uansett, fordi kelvin `motor` eksporterer det
 * med `api(...)`. Kjente brudd er det som fantes 30.10.2026; listene skal bare krympe.
 */
class LagdelingTest {

    private val flytfiler = produksjon.files.filter { it.moduleName == "flyt" }
    private val apifiler = produksjon.files.filter { it.moduleName == "api" }

    @Test
    fun `flyt importerer ikke dbconnect, Kafka eller Ktor`() {
        val forbudt = mapOf(
            "no.nav.aap.komponenter.dbconnect" to "dbconnect",
            "org.apache.kafka" to "kafka",
            "io.ktor" to "ktor",
        )
        sjekkRegel(
            sjekket = flytfiler,
            funn = flytfiler.flatMap { f ->
                f.imports.mapNotNull { import -> forbudt.entries.find { import.name.startsWith(it.key) }?.value }
                    .distinct().map { "${f.name}: $it" }
            },
            kjenteBrudd = setOf(
                "GjenopptakRepositoryImpl: dbconnect",
                "KafkaStreams: dbconnect",
                "KafkaStreams: kafka",
                "KafkaStreamsProperties: kafka",
                "MottakStream: kafka",
                "PostmottakLogInfoProvider: dbconnect",
                "StreamsException: kafka",
            ),
            slikRetterDu = "SQL hører hjemme i repository (*RepositoryImpl), Kafka-oppsett i app eller egen modul.",
        )
    }

    @Test
    fun `RepositoryImpl ligger i repository-modulen`() {
        val repositoryImpl = klasser.filter { it.name.endsWith("RepositoryImpl") && it.erKonkret() && it.moduleName != "lib-test" }
        sjekkRegel(
            sjekket = repositoryImpl,
            funn = repositoryImpl.filter { it.moduleName != "repository" }.map { "${it.moduleName}: ${it.name}" },
            kjenteBrudd = setOf(
                "flyt: GjenopptakRepositoryImpl",
            ),
            slikRetterDu = "Legg nye repositories i repository/, med grensesnittet i flyt ved siden av modellen.",
        )
    }

    @Test
    fun `RepositoryImplTest ligger i repository-modulen`() {
        val repositoryTester = tester.classes().filter { it.name.endsWith("RepositoryImplTest") }
        sjekkRegel(
            sjekket = repositoryTester,
            funn = repositoryTester.filter { it.moduleName != "repository" }.map { "${it.moduleName}: ${it.name}" },
            kjenteBrudd = setOf(
                "flyt: AvklarTemaRepositoryImplTest",
                "flyt: DigitaliseringsvurderingRepositoryImplTest",
                "flyt: SaksnummerRepositoryImplTest",
            ),
            slikRetterDu = "Legg testen for et RepositoryImpl i repository/src/test, ved siden av implementasjonen.",
        )
    }

    @Test
    fun `api importerer ikke Impl-klasser`() = sjekkRegel(
        sjekket = apifiler,
        funn = apifiler.flatMap { f ->
            f.imports.map { it.name.substringAfterLast(".") }.filter { it.endsWith("Impl") }.map { "${f.name}: $it" }
        },
        slikRetterDu = "Bruk grensesnittet og hent implementasjonen via repositoryProvider/gatewayProvider.",
    )

    @Test
    fun `konstruktører tar ikke inn RepositoryImpl- eller GatewayImpl-typer`() {
        val kandidater = klasser.filter { it.moduleName !in setOf("repository", "lib-test") }
        sjekkRegel(
            sjekket = kandidater,
            funn = kandidater.flatMap { k ->
                (listOfNotNull(k.primaryConstructor) + k.secondaryConstructors).flatMap { c ->
                    c.parameters.map { it.type.name.removeSuffix("?") }
                        .filter { it.endsWith("RepositoryImpl") || it.endsWith("GatewayImpl") }
                        .map { "${k.name}($it)" }
                }
            },
            slikRetterDu = "Bruk marker-grensesnittet som parametertype. repositoryProvider.provide() slår opp på " +
                "den deklarerte typen og feiler i runtime for *Impl.",
        )
    }
}
