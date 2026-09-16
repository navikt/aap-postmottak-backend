package no.nav.aap.postmottak.mottak

import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde
import no.nav.aap.postmottak.mottak.kafka.config.StreamsConfig
import no.nav.aap.postmottak.mottak.kafka.config.toMap
import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord

class JournalfoeringHendelseAvro(config: StreamsConfig) {
    val avroserdes: SpecificAvroSerde<JournalfoeringHendelseRecord>

    companion object {
        init {
            // Avro 1.12.2 innførte en sikkerhetssperre (AVRO-4189) som forbyr (de)serialisering
            // av klasser som ikke eksplisitt er tiltrodd. Vi må derfor eksplisitt tiltro pakken
            // til de genererte Avro-klassene vi bruker for Joark-hendelser.
            System.setProperty(
                "org.apache.avro.SERIALIZABLE_PACKAGES",
                JournalfoeringHendelseRecord::class.java.packageName
            )
        }
    }

    init {
        val schemaProperties = config.schemaRegistry.properties()
        val sslProperties = config.ssl.properties()
        avroserdes = SpecificAvroSerde<JournalfoeringHendelseRecord>()
        avroserdes.configure((schemaProperties.toMap() + sslProperties.toMap()), false)
    }
}