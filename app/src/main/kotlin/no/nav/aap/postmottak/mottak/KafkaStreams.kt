package no.nav.aap.postmottak.mottak


import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.mottak.config.ProcessingExceptionHandler
import no.nav.aap.postmottak.mottak.config.StreamsConfig
import no.nav.aap.postmottak.mottak.config.toMap
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.KafkaStreams.State.CREATED
import org.apache.kafka.streams.KafkaStreams.State.ERROR
import org.apache.kafka.streams.KafkaStreams.State.REBALANCING
import org.apache.kafka.streams.KafkaStreams.State.RUNNING
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.kstream.Consumed
import org.slf4j.LoggerFactory
import javax.sql.DataSource


private val log = LoggerFactory.getLogger(MottakListener::class.java)

private typealias TransactionalFuntion = (BehandlingRepository, FlytJobbRepository) -> Unit
private typealias TransactionWithResources = (DataSource, TransactionalFuntion) -> Unit

const val JOARK_TOPIC = "teamdokumenthandtering.aapen-dok-journalfoering"
private const val TEMA = "AAP"
private const val MOTTATT = "MOTTATT"
private const val EESSI = "EESSI"


interface Stream {
    fun ready(): Boolean
    fun live(): Boolean
    fun close()
    fun start()
}

class NoopStream : Stream {
    override fun ready() = true

    override fun live() = true

    override fun close() {}

    override fun start() {}

}

class MottakStream(topology: Topology, config: StreamsConfig, registry: MeterRegistry): Stream {
    val streams = KafkaStreams(topology, config.streamsProperties())


    init {
        streams.setUncaughtExceptionHandler(ProcessingExceptionHandler())
        KafkaStreamsMetrics(streams).bindTo(registry)
    }

    override fun ready(): Boolean = streams.state() in listOf(CREATED, REBALANCING, RUNNING)
    override fun live(): Boolean = streams.state() != ERROR
    override fun close() = streams.close()
    override fun start() = streams.start()

}

class MottakListener(
    config: StreamsConfig,
    private val dataSource: DataSource,
    private val getTransactionWithResources: TransactionWithResources = {dataSource, fn -> dataSource.transaction { fn(BehandlingRepositoryImpl(it), FlytJobbRepository(it)) }} // TODO :poop: vanskelig å teste, vurder å flytte Håndter journalpost til egen klasse
) {

    val topology: Topology
    val avroserde: SpecificAvroSerde<JournalfoeringHendelseRecord>

    init {
        val schemaProperties = config.schemaRegistry?.properties() ?: error("missing required schema config")
        val sslProperties = config.ssl?.properties() ?: error("missing required ssl config")
        avroserde = SpecificAvroSerde<JournalfoeringHendelseRecord>()
        avroserde.configure((schemaProperties.toMap() + sslProperties.toMap()), false)

        val streamBuilder = StreamsBuilder()
        streamBuilder.stream(JOARK_TOPIC, Consumed.with(Serdes.String(), avroserde))
            .filter { _, record -> record.temaNytt == TEMA}
            .filter { _, record -> record.journalpostStatus == MOTTATT }
            .filter { _, record -> record.mottaksKanal != EESSI }
            .mapValues { record -> record.journalpostId}
            .foreach{ _, record -> håndterJournalpost(record)}

        topology = streamBuilder.build()
    }

    private fun håndterJournalpost(
        journalpostId: Long,
    ) {
        log.info("Mottatt $journalpostId")
        getTransactionWithResources(dataSource) { behandlingRepository, flytJobbRepository ->
            val journalpostId = JournalpostId(journalpostId)
            val behandling = behandlingRepository.opprettBehandling(journalpostId)
            flytJobbRepository.leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandling.id.toLong()).medCallId()
            )
        }
    }

}
