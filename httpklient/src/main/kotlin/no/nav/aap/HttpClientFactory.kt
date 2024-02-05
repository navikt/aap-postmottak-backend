package no.nav.aap

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.jackson.*
import org.slf4j.LoggerFactory

private val SECURE_LOGGER = LoggerFactory.getLogger("secureLog")

object HttpClientFactory {

    fun createClient(): HttpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 25_000
            connectTimeoutMillis = 5_000
        }
        install(HttpRequestRetry)

        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) = SECURE_LOGGER.info(message)
            }
            level = LogLevel.ALL
        }

        install(ContentNegotiation) {
            jackson {
                disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                registerModule(JavaTimeModule())
            }
        }
    }

}