package no.nav.aap.postmottak.mottak

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.mottak.kafka.config.toMap
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class JournalfoeringHendelseAvro(config: StreamsConfig) {
    val avroserdes: SpecificAvroSerde<JournalfoeringHendelseRecord>

    init {
        val schemaProperties = config.schemaRegistry?.properties() ?: error("missing required schema config")
        val sslProperties = config.ssl?.properties() ?: error("missing required ssl config")
        avroserdes = SpecificAvroSerde<JournalfoeringHendelseRecord>()
        avroserdes.configure((schemaProperties.toMap() + sslProperties.toMap()), false)
    }
}