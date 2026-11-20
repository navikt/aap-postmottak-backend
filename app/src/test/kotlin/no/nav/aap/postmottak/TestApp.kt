@file:Suppress("KotlinPrintToLogpoint")

package no.nav.aap.postmottak

import com.papsign.ktor.openapigen.route.apiRouting
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.FakeServers
import no.nav.aap.postmottak.test.FakeUnleash
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer
import java.time.Duration
import java.time.temporal.ChronoUnit

// Kjøres opp for å få logback i console uten json
fun main() {
    val dbConfig = initDbConfig()
    FakeServers().start()

    val gatewayProvider = defaultGatewayProvider {
        register<FakeUnleash>()
    }

    // Starter server
    embeddedServer(Netty, port = 8070) {
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${dbConfig.url}. Password: ${dbConfig.password}. Username: ${dbConfig.username}.")
        server(
            dbConfig, postgresRepositoryRegistry, gatewayProvider
        )

        val datasource = initDatasource(dbConfig, SimpleMeterRegistry())

        // Lar en POST /test/simulerJournalpostHendelse simulere en journalføringshendelse fra
        // Kafka lokalt, uten ekte broker, se SimulerJournalpostHendelseRoute.kt.
        apiRouting {
            simulerJournalpostHendelseApi(datasource, postgresRepositoryRegistry, gatewayProvider)
        }
    }.start(wait = true)
}

private fun initDbConfig(): DbConfig {
    return if (System.getenv("DB_POSTMOTTAK_JDBC_URL").isNullOrBlank()) {
        val postgres = postgreSQLContainer()

        DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
    } else {
        DbConfig()
    }.also {
        println("----\nDATABASE URL: \n${it.url}?user=${it.username}&password=${it.password}\n----")
    }
}

internal fun postgreSQLContainer(): PostgreSQLContainer {
    val postgres = PostgreSQLContainer("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}
