package no.nav.aap.postmottak.fordeler.arena

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer


object ProducerProvider {

    fun provideProducer(): Producer<String, String> {
        return KafkaProducer(ArenaProducerConfig().getConfig())
    }

}