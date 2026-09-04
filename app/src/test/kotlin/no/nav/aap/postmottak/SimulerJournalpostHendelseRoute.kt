package no.nav.aap.postmottak

import com.papsign.ktor.openapigen.route.path.normal.NormalOpenAPIRoute
import com.papsign.ktor.openapigen.route.path.normal.post
import com.papsign.ktor.openapigen.route.response.respondWithStatus
import com.papsign.ktor.openapigen.route.route
import io.ktor.http.HttpStatusCode
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.mottak.JOARK_TOPIC
import no.nav.aap.postmottak.mottak.JoarkKafkaHandler
import no.nav.aap.postmottak.mottak.JournalfoeringHendelseAvro
import no.nav.aap.postmottak.mottak.kafka.config.SchemaRegistryConfig
import no.nav.aap.postmottak.mottak.kafka.config.SslConfig
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("no.nav.aap.postmottak.SimulerJournalpostHendelseRoute")

/**
 * Forhåndsdefinerte scenarioer som dekker de vanligste [no.nav.aap.postmottak.test.fakes.TestJournalPostBuilder]-
 * oppsettene, slik at man enkelt kan simulere de samme journalpostene lokalt som i tester.
 */
enum class SimulertJournalpostScenario {
    // Digital legeerklæring. Dette er standardoppsettet til TestJournalPostBuilder (brevkode LEGEERKLÆRING).
    LEGEERKLÆRING,

    // Tilsvarer TestJournalPostBuilder.digitalSøknad().
    DIGITAL_SØKNAD,

    // Tilsvarer TestJournalPostBuilder.papirsøknad().
    PAPIRSØKNAD,

    // Tilsvarer TestJournalPostBuilder.medUtenlandskOrgnr(). NB: Kelvin krever at journalposter med
    // orgnr som bruker allerede er journalført (se JournalpostInformasjonskrav), så journalpostStatus
    // må settes til JOURNALFOERT for at behandlingen skal gå gjennom flyten uten feil.
    UTENLANDSK_ORGNR,
}

/**
 * Kun for lokal testing i TestApp: simulerer en journalføringshendelse fra Kafka uten å trenge en
 * ekte Kafka-broker. Kjører den samme topologien (filtrering + fordelingslogikk) som
 * [no.nav.aap.postmottak.mottak.kafka.MottakStream] ville gjort i prod, men via en
 * [TopologyTestDriver] mot den ekte lokale databasen og de vanlige fakene.
 *
 * Registrerer i tillegg journalposten (og evt. person/fnr) i [TestJournalposter] slik at de andre
 * fakene (SAF, NOM, PDL) svarer konsistent på oppslag for denne journalpostId-en, på samme måte
 * som når en journalpost opprettes via `TestJournalposter.leggTil { ... }` i tester.
 *
 */
data class SimulerJournalpostHendelseRequest(
    val journalpostId: Long,
    // Fødselsnummeret journalposten skal knyttes til. Dersom denne ikke settes opprettes det
    // automatisk en ny testperson (se TestJournalposter.leggTil). Brukes ikke ved scenario UTENLANDSK_ORGNR.
    val fnr: String? = null,
    val tema: String = "AAP",
    val temaGammelt: String? = null,
    val journalpostStatus: String = "MOTTATT",
    val hendelsesType: String = "JournalpostMottatt",
    // Se [SimulertJournalpostScenario] for hva de ulike scenarioene setter opp
    // (brevkode, kanal, digital/papir-dokumenter osv.).
    val scenario: SimulertJournalpostScenario = SimulertJournalpostScenario.LEGEERKLÆRING,
)

fun NormalOpenAPIRoute.simulerJournalpostHendelseApi(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    route("/test/simulerJournalpostHendelse") {
        post<Unit, Unit, SimulerJournalpostHendelseRequest> { _, request ->
            log.info("Simulerer journalføringshendelse for journalpostId={}", request.journalpostId)

            val journalpost = registrerTestJournalpost(request)
            simulerJournalpostHendelse(dataSource, repositoryRegistry, gatewayProvider, request, journalpost.kanal)

            respondWithStatus(HttpStatusCode.NoContent)
        }
    }
}

private fun registrerTestJournalpost(request: SimulerJournalpostHendelseRequest): TestJournalPost {
    return TestJournalposter.leggTil {
        journalpostId = request.journalpostId
        fnr = request.fnr
        tema = request.tema
        status = Journalstatus.valueOf(request.journalpostStatus)
        when (request.scenario) {
            // Standardoppsettet til builderen er allerede en digital legeerklæring.
            SimulertJournalpostScenario.LEGEERKLÆRING -> Unit
            SimulertJournalpostScenario.DIGITAL_SØKNAD -> digitalSøknad()
            SimulertJournalpostScenario.PAPIRSØKNAD -> papirsøknad()
            SimulertJournalpostScenario.UTENLANDSK_ORGNR -> medUtenlandskOrgnr()
        }
    }
}

private fun simulerJournalpostHendelse(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    request: SimulerJournalpostHendelseRequest,
    kanal: KanalFraKodeverk,
) {
    val config = StreamsConfig(
        applicationId = "postmottak-simuler-lokalt",
        brokers = "",
        ssl = SslConfig(truststorePath = "", keystorePath = "", credstorePsw = ""),
        schemaRegistry = SchemaRegistryConfig(url = "mock://postmottak-testapp", user = "", password = ""),
    )

    val joarkKafkaHandler = JoarkKafkaHandler(config, dataSource, repositoryRegistry, gatewayProvider)
    val topologyTestDriver = TopologyTestDriver(joarkKafkaHandler.topology, config.streamsProperties())

    val record = JournalfoeringHendelseRecord.newBuilder().apply {
        hendelsesId = UUID.randomUUID().toString()
        versjon = 1
        hendelsesType = request.hendelsesType
        journalpostId = request.journalpostId
        temaGammelt = request.temaGammelt ?: request.tema
        temaNytt = request.tema
        journalpostStatus = request.journalpostStatus
        mottaksKanal = kanal.name
        kanalReferanseId = ""
        behandlingstema = ""
    }.build()

    topologyTestDriver.createInputTopic(
        JOARK_TOPIC,
        Serdes.String().serializer(),
        JournalfoeringHendelseAvro(config).avroserdes.serializer()
    ).pipeInput(UUID.randomUUID().toString(), record)

    topologyTestDriver.close()
}
