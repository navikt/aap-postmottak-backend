package no.nav.aap.postmottak.mottak

import io.ktor.server.application.*
import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.postmottak.mottak.kafka.MottakStream
import no.nav.aap.postmottak.mottak.kafka.NoopStream
import no.nav.aap.postmottak.mottak.kafka.Stream
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import javax.sql.DataSource

fun Application.mottakStream(dataSource: DataSource, registry: MeterRegistry): Stream {
    if (Miljø.er() == MiljøKode.LOKALT) return NoopStream()
    val config = StreamsConfig()
    val stream = MottakStream(JoarkKafkaHandler(config, dataSource).topology, config, registry)
    stream.start()
    monitor.subscribe(ApplicationStopped) {
        stream.close()
    }
    return stream
}