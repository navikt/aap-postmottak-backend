package no.nav.aap.postmottak.mottak


import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource


class TransactionContext(
    val behandlingRepository: BehandlingRepository,
    val flytJobbRepository: FlytJobbRepository,
)

class TransactionProvider(
    val datasource: DataSource
) {
    fun inTransaction(block: TransactionContext.() -> Unit) {
        datasource.transaction {
            TransactionContext(BehandlingRepositoryImpl(it), FlytJobbRepository(it)).let(block)
        }
    }
}


const val JOARK_TOPIC = "teamdokumenthandtering.aapen-dok-journalfoering"
private const val TEMA = "AAP"
private const val MOTTATT = "MOTTATT"
private const val EESSI = "EESSI"


class JoarkKafkaHandler(
    config: StreamsConfig,
    datasource: DataSource,
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource)
) {

    private val log = LoggerFactory.getLogger(JoarkKafkaHandler::class.java)

    val topology: Topology

    init {
        val journalfoeringHendelseAvro: JournalfoeringHendelseAvro = JournalfoeringHendelseAvro(config)
        val streamBuilder = StreamsBuilder()
        streamBuilder.stream(JOARK_TOPIC, Consumed.with(Serdes.String(), journalfoeringHendelseAvro.avroserdes))
            .filter { _, record -> record.temaNytt == TEMA }
            .filter { _, record -> record.journalpostStatus == MOTTATT }
            .filter { _, record -> record.mottaksKanal != EESSI }
            .mapValues { record -> record.journalpostId }
            .foreach { _, record -> håndterJournalpost(record) }

        topology = streamBuilder.build()
    }

    private fun håndterJournalpost(
        journalpostId: Long,
    ) {
        log.info("Mottatt $journalpostId")
        transactionProvider.inTransaction {
            val journalpostId = JournalpostId(journalpostId)
            val behandling = behandlingRepository.opprettBehandling(journalpostId)
            flytJobbRepository.leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandling.id.toLong()).medCallId()
            )
        }
    }

}
