package no.nav.aap.postmottak.mottak


import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.hendelseType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.JoarkRegel.erIkkeKanalEESSI
import no.nav.aap.postmottak.mottak.JoarkRegel.erTemaAAP
import no.nav.aap.postmottak.mottak.JoarkRegel.erTemaEndretFraAAP
import no.nav.aap.postmottak.mottak.JoarkRegel.harStatusMottatt
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.unleash.UnleashGateway
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Branched
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.slf4j.LoggerFactory
import javax.sql.DataSource


class TransactionContext(
    val behandlingRepository: BehandlingRepository,
    val flytJobbRepository: FlytJobbRepository,
    val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator
)

class TransactionProvider(
    val datasource: DataSource,
    val repositoryRegistry: RepositoryRegistry,
    val gatewayProvider: GatewayProvider,
) {
    fun <A> inTransaction(readOnly: Boolean = false, block: TransactionContext.() -> A): A {
        return datasource.transaction(readOnly = readOnly) {
            val provider = repositoryRegistry.provider(it)
            TransactionContext(
                provider.provide(),
                provider.provide(),
                AvklaringsbehovOrkestrator(provider, gatewayProvider)
            ).let(block)
        }
    }
}


const val JOARK_TOPIC = "teamdokumenthandtering.aapen-dok-journalfoering"

/**
 * Dokumentasjon på hendelsene finnes på [Confluence](https://confluence.adeo.no/spaces/BOA/pages/432217891/Joarkhendelser)
 */
class JoarkKafkaHandler(
    config: StreamsConfig,
    datasource: DataSource,
    repositoryRegistry: RepositoryRegistry,
    gatewayProvider: GatewayProvider,
    private val transactionProvider: TransactionProvider = TransactionProvider(
        datasource,
        repositoryRegistry,
        gatewayProvider
    ),
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
) {

    private val log = LoggerFactory.getLogger(JoarkKafkaHandler::class.java)

    val topology: Topology

    val unleashGateway: UnleashGateway

    init {
        val journalfoeringHendelseAvro = JournalfoeringHendelseAvro(config)
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(JOARK_TOPIC, Consumed.with(Serdes.String(), journalfoeringHendelseAvro.avroserdes))
            .filter(harStatusMottatt)
            .filter(erIkkeKanalEESSI)
            .peek { _, record -> prometheus.hendelseType(record) }
            .split()
            .branch(erTemaEndretFraAAP, Branched.withConsumer(::temaendringTopology))
            .branch(erTemaAAP, Branched.withConsumer(::nyJournalpost))

        topology = streamBuilder.build()

        unleashGateway = gatewayProvider.provide()
    }

    private fun temaendringTopology(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.mapValues { record -> JournalpostId(record.journalpostId) }
            .foreach { _, record -> håndterTemaendring(record) }
    }

    private fun nyJournalpost(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.foreach { _, record ->
            val journalpostId = record.journalpostId.let(::JournalpostId)
            val åpenBehandling = transactionProvider.inTransaction(readOnly = true) {
                behandlingRepository.hentÅpenJournalføringsbehandling(journalpostId)
            }
            if (åpenBehandling != null) {
                transactionProvider.inTransaction {
                    avklaringsbehovOrkestrator.taAvVentPgaGosys(åpenBehandling.id)
                    triggProsesserBehandling(
                        journalpostId,
                        åpenBehandling
                    )
                }
            } else {
                opprettFordelingRegelJobb(journalpostId, record.hendelsesType)
            }
        }
    }

    /**
     * Skjer om journalposten har endret tema fra AAP. Dette er for å trigge en tema-endring-behandling.
     */
    private fun håndterTemaendring(journalpostId: JournalpostId) {
        log.info("Mottatt temaendring på journalpost $journalpostId")
        transactionProvider.inTransaction {
            val behandling = behandlingRepository.hentÅpenJournalføringsbehandling(journalpostId)
            if (behandling != null) {
                triggProsesserBehandling(journalpostId, behandling)
            } else {
                log.info("Fant ikke åpen behandling for journalpost $journalpostId")
            }
        }
    }

    private fun TransactionContext.triggProsesserBehandling(
        journalpostId: JournalpostId,
        behandling: Behandling,
    ) {
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(sakID = journalpostId.referanse, behandlingId = behandling.id.id)
                .medCallId()
        )
    }

    private fun opprettFordelingRegelJobb(
        journalpostId: JournalpostId,
        hendelse: String,
    ) {
        log.info("Mottatt ny journalpost: $journalpostId, hendelse: $hendelse")
        transactionProvider.inTransaction {
            flytJobbRepository.leggTil(
                JobbInput(FordelingRegelJobbUtfører)
                    .forSak(journalpostId.referanse)
                    .medJournalpostId(journalpostId)
            )
        }
    }

}

object JoarkRegel {
    // Mulige verdier for tema: https://confluence.adeo.no/spaces/BOA/pages/316396024/Tema
    private const val TEMA_AAP = "AAP"
    private const val MOTTATT = "MOTTATT"
    private const val EESSI = "EESSI"

    val harStatusMottatt: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.journalpostStatus == MOTTATT }

    /**
     * Mottakskanal er dokumentert [her](https://confluence.adeo.no/spaces/BOA/pages/316396050/Mottakskanal).
     */
    val erIkkeKanalEESSI: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.mottaksKanal != EESSI }

    val erTemaEndretFraAAP: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.temaGammelt == TEMA_AAP && record.temaNytt != TEMA_AAP }

    val erTemaAAP: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.temaNytt == TEMA_AAP }

}
