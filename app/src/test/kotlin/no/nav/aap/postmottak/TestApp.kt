package no.nav.aap.postmottak

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
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
            opprettBehandlingAvklarTeam(it)
            opprettBehandlingFinnSak(it)
            opprettBehandlingKategoriser(it)
            opprettBehandlingDigitaliser(it)
        }

    }.start(wait = true)
}

private fun opprettBehandlingAvklarTeam(connection: DBConnection) {
    val behandling = BehandlingRepositoryImpl(connection).opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)

    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(1, behandling.id).medCallId()
    )
}

private fun opprettBehandlingFinnSak(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(2)
    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

private fun opprettBehandlingKategoriser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(3)

    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
    SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

private fun opprettBehandlingDigitaliser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(4)
    val behandlingId =
        behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
    SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
    KategorivurderingRepository(connection).lagreKategoriseringVurdering(behandlingId, InnsendingType.SØKNAD)
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

internal fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:15")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}

private fun Application.module(fakes: Fakes) {
    // Setter opp virtuell sandkasse lokalt
    monitor.subscribe(ApplicationStopped) { application ->
        application.environment.log.info("Server har stoppet")
        fakes.close()
        // Release resources and unsubscribe from events
        application.monitor.unsubscribe(ApplicationStopped) {}
    }
}
