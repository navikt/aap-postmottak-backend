package no.nav.aap.postmottak.mottak


import io.ktor.server.sessions.*
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.hendelse.oppgave.OppgaveGateway
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.flate.BehandlingReferanseService
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
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
    val flytJobbRepository: FlytJobbRepository
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
        val journalfoeringHendelseAvro = JournalfoeringHendelseAvro(config)
        val streamBuilder = StreamsBuilder()
        val stream = streamBuilder.stream(JOARK_TOPIC, Consumed.with(Serdes.String(), journalfoeringHendelseAvro.avroserdes))
            .filter { _, record -> record.journalpostStatus == MOTTATT }
            .filter { _, record -> record.mottaksKanal != EESSI }
            .split()
            .branch({_, record -> record.temaGammelt == TEMA && record.temaNytt != TEMA}, Branched.withConsumer(::temaendringTopology))
            .branch({ _, record -> record.temaNytt == TEMA}, Branched.withConsumer(::nyJournalpost))

        topology = streamBuilder.build()
    }

    private fun temaendringTopology(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.mapValues { record -> JournalpostId(record.journalpostId) }
            .foreach{ _, record -> håndterTemaendring(record) }
    }

    private fun nyJournalpost(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.mapValues { record -> JournalpostId(record.journalpostId) }
            .foreach { _, record -> håndterJournalpost(record) }
    }

    private fun håndterTemaendring(journalpostId: JournalpostId) {
        transactionProvider.inTransaction {
            val behandlingReferanseService = BehandlingReferanseService(behandlingRepository)
            try {
                val behandling = behandlingReferanseService.behandling(journalpostId)
                flytJobbRepository.leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører)
                        .forBehandling(null, behandling.id.id).medCallId()
                )
            } catch (e: ElementNotFoundException) {
                log.warn("Finner ikke behandling for mottatt melding om temaendring", e)
            }

        }
    }

    private fun håndterJournalpost(journalpostId: JournalpostId) {
        log.info("Mottatt $journalpostId")
        transactionProvider.inTransaction {
            val behandling = behandlingRepository.opprettBehandling(journalpostId)
            flytJobbRepository.leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandling.id).medCallId()
            )
        }
    }


}
