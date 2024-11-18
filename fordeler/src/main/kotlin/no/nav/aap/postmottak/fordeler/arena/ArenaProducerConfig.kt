package no.nav.aap.postmottak.fordeler.arena

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig
import no.nav.aap.komponenter.config.requiredConfigForKey
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class ArenaProducerConfig {

    fun getConfig() = Properties().apply {

        putAll(getSslConfig().toMap())

        this[ProducerConfig.BOOTSTRAP_SERVERS_CONFIG] = requiredConfigForKey("KAFKA_BROKERS")

        this[ProducerConfig.ACKS_CONFIG] = "all"

        this[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java.name
        this[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = KafkaAvroSerializer::class.java.name

        this[KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG] = requiredConfigForKey("KAFKA_SCHEMA_REGISTRY")
        this[KafkaAvroSerializerConfig.BASIC_AUTH_CREDENTIALS_SOURCE] = KafkaAvroSerializerConfig.USER_INFO_CONFIG
        this[KafkaAvroSerializerConfig.USER_INFO_CONFIG] = "USER_INFO"
            "${requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_USER")}:${requiredConfigForKey("KAFKA_SCHEMA_REGISTRY_PASSWORD")}"


        // Configuration for decreaseing latency
        this[ProducerConfig.BATCH_SIZE_CONFIG] = 0
        this[ProducerConfig.LINGER_MS_CONFIG] = 0

    }

    fun getSslConfig() = Properties().apply {
            val credstorePsw = requiredConfigForKey("KAFKA_CREDSTORE_PASSWORD")
            this[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
            this[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = "JKS"
            this[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = requiredConfigForKey("KAFKA_TRUSTSTORE_PATH")
            this[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = credstorePsw
            this[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = "PKCS12"
            this[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = requiredConfigForKey("KAFKA_KEYSTORE_PATH")
            this[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = credstorePsw
            this[SslConfigs.SSL_KEY_PASSWORD_CONFIG] = credstorePsw
            this[SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG] = ""
        }


}