package no.nav.aap.postmottak

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.PAPIR_SØKNAD
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

        val datasource = initDatasource(dbConfig, SimpleMeterRegistry())

        datasource.transaction {
            opprettBehandlingAvklarTeam(it)
            opprettBehandlingFinnSak(it)
            opprettBehandlingKategoriser(it)
            opprettBehandlingDigitaliser(it)
            opprettBehandlingPapirSøknadKategoriser(it)
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
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

private fun opprettBehandlingKategoriser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(3)

    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
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
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

private fun opprettBehandlingPapirSøknadKategoriser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(PAPIR_SØKNAD.referanse)

    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

internal fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
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
