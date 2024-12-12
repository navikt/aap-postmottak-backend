package no.nav.aap.postmottak.mottak


import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.server.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.server.prosessering.medJournalpostId
import no.nav.aap.verdityper.feilhåndtering.ElementNotFoundException
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
)

class TransactionProvider(
    val datasource: DataSource
) {
    fun inTransaction(block: TransactionContext.() -> Unit) {
        datasource.transaction {
            TransactionContext(
                BehandlingRepositoryImpl(it),
                FlytJobbRepository(it),
            ).let(block)
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
        streamBuilder.stream(JOARK_TOPIC, Consumed.with(Serdes.String(), journalfoeringHendelseAvro.avroserdes))
            .filter { _, record -> record.journalpostStatus == MOTTATT }
            .filter { _, record -> record.mottaksKanal != EESSI }
            .split()
            .branch(
                { _, record -> record.temaGammelt == TEMA && record.temaNytt != TEMA },
                Branched.withConsumer(::temaendringTopology)
            )
            .branch({ _, record -> record.temaNytt == TEMA }, Branched.withConsumer(::nyJournalpost))

        topology = streamBuilder.build()
    }

    private fun temaendringTopology(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.mapValues { record -> JournalpostId(record.journalpostId) }
            .foreach { _, record -> håndterTemaendring(record) }
    }

    private fun nyJournalpost(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.foreach { _, record ->
            opprettFordelingRegelJobb(record.journalpostId.let(::JournalpostId))
        }
    }

    private fun håndterTemaendring(journalpostId: JournalpostId) {
        log.info("Motatt temaendring på journalpost $journalpostId")
        transactionProvider.inTransaction {
            try {
                val behandling = behandlingRepository.hentÅpenJournalføringsbehandling(journalpostId)
                flytJobbRepository.leggTil(
                    JobbInput(ProsesserBehandlingJobbUtfører)
                        .forBehandling(sakID = journalpostId.referanse, behandlingId = behandling.id.id).medCallId()
                )
            } catch (e: Exception) {
                when (e) {
                    is ElementNotFoundException, is NoSuchElementException -> log.info("Fant ikke åpen behandling for journalpost $journalpostId")
                    else -> throw e
                }
            }
        }
    }

    private fun opprettFordelingRegelJobb(
        journalpostId: JournalpostId
    ) {
        log.info("Mottatt ny journalpost: $journalpostId")
        transactionProvider.inTransaction {

            flytJobbRepository.leggTil(
                JobbInput(FordelingRegelJobbUtfører) 
                    .forSak(journalpostId.referanse)
                    .medJournalpostId(journalpostId)
            )
        }
    }

}
