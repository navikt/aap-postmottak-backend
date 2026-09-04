package no.nav.aap.postmottak

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.mottak.JOARK_TOPIC
import no.nav.aap.postmottak.mottak.JoarkKafkaHandler
import no.nav.aap.postmottak.mottak.JournalfoeringHendelseAvro
import no.nav.aap.postmottak.mottak.kafka.config.SchemaRegistryConfig
import no.nav.aap.postmottak.mottak.kafka.config.SslConfig
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.TopologyTestDriver
import org.slf4j.LoggerFactory
import java.util.UUID
import javax.sql.DataSource

private val log = LoggerFactory.getLogger("no.nav.aap.postmottak.SimulerJournalpostHendelseRoute")

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
 * Ligger bevisst i test-kildesettet (kun brukt av TestApp) slik at test-avhengigheten
 * kafka-streams-test-utils ikke havner i produksjonsjaren, og krever derfor ingen autentisering.
 */
data class SimulerJournalpostHendelseRequest(
    val journalpostId: Long,
    // Fødselsnummeret journalposten skal knyttes til. Dersom denne ikke settes opprettes det
    // automatisk en ny testperson (se TestJournalposter.leggTil).
    val fnr: String? = null,
    val tema: String = "AAP",
    val temaGammelt: String? = null,
    val journalpostStatus: String = "MOTTATT",
    val mottaksKanal: String = "NAV_NO",
    val hendelsesType: String = "JournalpostMottatt",
    // Om journalposten skal simuleres som en digital søknad, dvs. samme oppsett som
    // TestJournalPostBuilder.digitalSøknad() gir i tester (brevkode SØKNAD + en digital SøknadV0).
    val erDigitalSøknad: Boolean = false,
)

fun Application.installSimulerJournalpostHendelseRoute(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
) {
    routing {
        post("/test/simulerJournalpostHendelse") {
            val request = DefaultJsonMapper.fromJson<SimulerJournalpostHendelseRequest>(call.receiveText())
            log.info("Simulerer journalføringshendelse for journalpostId={}", request.journalpostId)

            registrerTestJournalpost(request)
            simulerJournalpostHendelse(dataSource, repositoryRegistry, gatewayProvider, request)

            call.respond(HttpStatusCode.NoContent)
        }
    }
}

private fun registrerTestJournalpost(request: SimulerJournalpostHendelseRequest) {
    TestJournalposter.leggTil {
        journalpostId = request.journalpostId
        fnr = request.fnr
        tema = request.tema
        status = Journalstatus.valueOf(request.journalpostStatus)
        kanal = KanalFraKodeverk.valueOf(request.mottaksKanal)
        if (request.erDigitalSøknad) {
            digitalSøknad()
        }
    }
}

private fun simulerJournalpostHendelse(
    dataSource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    request: SimulerJournalpostHendelseRequest,
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
        mottaksKanal = request.mottaksKanal
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
