package no.nav.aap.postmottak.flyt

import org.slf4j.LoggerFactory
import org.testcontainers.containers.output.Slf4jLogConsumer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.kafka.KafkaContainer
import org.testcontainers.utility.DockerImageName
import java.time.Duration

internal object SharedKafkaTestContainer {
    private val logger = LoggerFactory.getLogger(SharedKafkaTestContainer::class.java)

    val kafka: KafkaContainer by lazy {
        KafkaContainer(DockerImageName.parse("apache/kafka-native:4.1.0"))
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(180))
            .withLogConsumer { Slf4jLogConsumer(logger) }
            .also { it.start() }
    }
}
