package no.nav.aap.postmottak.mottak.kafka

import io.micrometer.core.instrument.binder.kafka.KafkaStreamsMetrics
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.mottak.kafka.config.ProcessingExceptionHandler
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import org.apache.kafka.shaded.com.google.protobuf.Option
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.Topology
import java.util.Optional
import kotlin.time.Duration
import kotlin.time.toJavaDuration

class MottakStream(topology: Topology, config: StreamsConfig): Stream {
    val streams = KafkaStreams(topology, config.streamsProperties())
    
    init {
        streams.setUncaughtExceptionHandler(ProcessingExceptionHandler())
        KafkaStreamsMetrics(streams).bindTo(PrometheusProvider.prometheus)
    }

    override fun ready(): Boolean = streams.state() in listOf(
        KafkaStreams.State.CREATED,
        KafkaStreams.State.REBALANCING,
        KafkaStreams.State.RUNNING
    )
    override fun live(): Boolean = streams.state() != KafkaStreams.State.ERROR
    override fun close() = streams.close()
    override fun close(timeout: Duration) = streams.close(timeout.toJavaDuration())
    override fun start() = streams.start()

}