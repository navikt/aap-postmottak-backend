package no.nav.aap.postmottak.fordeler.arena

import no.nav.joarkjournalfoeringhendelser.JournalfoeringHendelseRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer


object ProducerProvider {

    fun provideProducer(): Producer<String, JournalfoeringHendelseRecord> {
        return KafkaProducer(ArenaProducerConfig().getConfig())
    }

}