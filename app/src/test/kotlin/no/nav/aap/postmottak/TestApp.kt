package no.nav.aap.postmottak

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.test.Fakes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

class TestApp {
    @Test
    @EnabledIfEnvironmentVariable(named = "TEST_APP", matches = "RUN")
    fun `kjør test-app`() {
        // For å kjøre test-app fra kommandolinjen, kjør
        // cd app && TEST_APP=RUN ../gradlew test --tests TestApp --info
        main()
    }
}

// Kjøres opp for å få logback i console uten json
fun main() {
    System.setProperty("NAIS_CLUSTER_NAME", "LOCAL")

    val postgres = postgreSQLContainer()
    val fakes = Fakes(azurePort = 8081)

    // Starter server
    embeddedServer(Netty, port = 8080) {
        val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        server(
            dbConfig
        )
        module(fakes)

        val datasource = initDatasource(dbConfig)

        datasource.transaction {
            opprettBehanldingAvklarTeam(it)
            opprettBehanldingFinnSak(it)
            opprettBehanldingKategoriser(it)
            opprettBehanldingDigitaliser(it)
        }

    }.start(wait = true)
}

private fun opprettBehanldingAvklarTeam(connection: DBConnection) {
    val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(JournalpostId(1))

    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(null, behandling.id.toLong()).medCallId()
    )
}

private fun opprettBehanldingFinnSak(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val vurderingRepository = AvklaringRepositoryImpl(connection)

    val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(2))
    vurderingRepository.lagreTeamAvklaring(behandlingId, true)
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(null, behandlingId.toLong()).medCallId()
    )

}

private fun opprettBehanldingKategoriser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val vurderingRepository =
        AvklaringRepositoryImpl(connection)

    val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(3))
    vurderingRepository.lagreTeamAvklaring(behandlingId, true)
    vurderingRepository.lagreSakVurdering(behandlingId, Saksnummer("1010"))
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(null, behandlingId.toLong()).medCallId()
    )

}

private fun opprettBehanldingDigitaliser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val vurderingRepository = AvklaringRepositoryImpl(connection)

    val behandlingId =
        behandlingRepository.opprettBehandling(JournalpostId(4))
    vurderingRepository.lagreTeamAvklaring(behandlingId, true)
    vurderingRepository.lagreSakVurdering(behandlingId, Saksnummer("1010"))
    vurderingRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(null, behandlingId.toLong()).medCallId()
    )

}

private fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}

private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    environment.monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.environment.monitor.unsubscribe(ApplicationStopped) {}
    }
}
