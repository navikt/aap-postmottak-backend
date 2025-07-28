package no.nav.aap.postmottak.mottak


import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.httpklient.exception.VerdiIkkeFunnetException
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.hendelseType
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.JoarkRegel.erIkkeKanalEESSI
import no.nav.aap.postmottak.mottak.JoarkRegel.erTemaAAP
import no.nav.aap.postmottak.mottak.JoarkRegel.harStatusJournalført
import no.nav.aap.postmottak.mottak.JoarkRegel.harStatusMottatt
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.OppryddingJobbUtfører
import no.nav.aap.postmottak.prosessering.medJournalpostId
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
                RepositoryProvider(it).provide(BehandlingRepository::class),
                FlytJobbRepository(it),
            ).let(block)
        }
    }
}


const val JOARK_TOPIC = "teamdokumenthandtering.aapen-dok-journalfoering"

class JoarkKafkaHandler(
    config: StreamsConfig,
    datasource: DataSource,
    private val transactionProvider: TransactionProvider = TransactionProvider(datasource),
    private val prometheus: MeterRegistry = SimpleMeterRegistry(),
) {

    private val log = LoggerFactory.getLogger(JoarkKafkaHandler::class.java)

    val topology: Topology

    init {
        val journalfoeringHendelseAvro = JournalfoeringHendelseAvro(config)
        val streamBuilder = StreamsBuilder()

        streamBuilder.stream(JOARK_TOPIC, Consumed.with(Serdes.String(), journalfoeringHendelseAvro.avroserdes))
            .filter(erTemaAAP)
            .filter(erIkkeKanalEESSI)
            .peek { _, record -> prometheus.hendelseType(record) }
            .split()
            .branch(harStatusMottatt, Branched.withConsumer(::nyJournalpost))
            .branch(harStatusJournalført, Branched.withConsumer(::ferdigstiltJournalpost))

        topology = streamBuilder.build()
    }

    private fun nyJournalpost(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.foreach { _, record ->
            opprettFordelingRegelJobb(record.journalpostId.let(::JournalpostId), record.hendelsesType)
        }
    }

    private fun ferdigstiltJournalpost(stream: KStream<String, JournalfoeringHendelseRecord>) {
        stream.foreach { _, record ->
            opprettOppryddingJobb(record.journalpostId.let(::JournalpostId), record.hendelsesType)
        }
    }

    private fun opprettOppryddingJobb(journalpostId: JournalpostId, hendelsesType: String) {
        log.info("Mottok journalført journalpost $journalpostId, hendelsesType: $hendelsesType")

        transactionProvider.inTransaction {
            try {
                flytJobbRepository.leggTil(
                    JobbInput(OppryddingJobbUtfører)
                        .forSak(journalpostId.referanse)
                        .medJournalpostId(journalpostId)
                )
            } catch (e: Exception) {
                when (e) {
                    is VerdiIkkeFunnetException,
                    is NoSuchElementException -> log.info("Fant ikke åpen behandling for journalpost $journalpostId")
                    else -> throw e
                }
            }
        }
    }

    private fun opprettFordelingRegelJobb(
        journalpostId: JournalpostId,
        hendelsesType: String,
    ) {
        log.info("Mottatt ny journalpost: $journalpostId, hendelsesType: $hendelsesType")

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
    private const val TEMA_AAP = "AAP"
    private const val MOTTATT = "MOTTATT"
    private const val EESSI = "EESSI"

    val harStatusMottatt: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.journalpostStatus == MOTTATT }

    val erIkkeKanalEESSI: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.mottaksKanal != EESSI }

    val erTemaEndretFraAAP: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.temaGammelt == TEMA_AAP && record.temaNytt != TEMA_AAP }

    val erTemaAAP: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.temaNytt == TEMA_AAP }

    val harStatusJournalført: (String, JournalfoeringHendelseRecord) -> Boolean =
        { _, record -> record.journalpostStatus == "JOURNALFOERT" }
}
