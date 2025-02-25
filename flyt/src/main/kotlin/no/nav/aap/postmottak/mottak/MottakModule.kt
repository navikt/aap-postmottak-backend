package no.nav.aap.postmottak.mottak

import io.ktor.server.application.*
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.komponenter.miljo.MiljøKode
import no.nav.aap.postmottak.mottak.kafka.MottakStream
import no.nav.aap.postmottak.mottak.kafka.NoopStream
import no.nav.aap.postmottak.mottak.kafka.Stream
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import javax.sql.DataSource

fun Application.mottakStream(dataSource: DataSource): Stream {
    if (Miljø.er() == MiljøKode.LOKALT) return NoopStream()
    //if (Miljø.er() == MiljøKode.PROD) return NoopStream()
    val config = StreamsConfig()
    val stream = MottakStream(JoarkKafkaHandler(config, dataSource).topology, config)
    stream.start()
    monitor.subscribe(ApplicationStopped) {
        stream.close()
    }
    return stream
}