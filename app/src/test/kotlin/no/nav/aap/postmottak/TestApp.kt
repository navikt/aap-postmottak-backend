@file:Suppress("KotlinPrintToLogpoint")

package no.nav.aap.postmottak

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.FakeServers
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestArenaSak
import no.nav.aap.postmottak.test.modell.TestArenaVedtak
import no.nav.aap.postmottak.test.modell.TestPersoner
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.LocalDate
import java.time.temporal.ChronoUnit

// Kjøres opp for å få logback i console uten json
fun main() {
    val dbConfig = initDbConfig()
    FakeServers().start()

    // Starter server
    embeddedServer(Netty, port = 8070) {
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${dbConfig.url}. Password: ${dbConfig.password}. Username: ${dbConfig.username}.")
        server(
            dbConfig, postgresRepositoryRegistry, defaultGatewayProvider()
        )

        val datasource = initDatasource(dbConfig, SimpleMeterRegistry())

        datasource.transaction {
            opprettBehandlingAvklarTeam(it)
            opprettBehandlingFinnSak(it)
            opprettBehandlingKategoriser(it)
            opprettBehandlingDigitaliser(it)
            opprettBehandlingPapirSøknadKategoriser(it)
            opprettBehandlingManuellFordeling(it)
            opprettBehandlingManuellFordelingDigitalSøknad(it)
            opprettBehandlingManuellFordelingPapirSøknad(it)
        }

    }.start(wait = true)
}

// Vedtak med maxdatoAap innen 20 uker etter journalpostens mottattDato (DATO_REGISTRERT = 2020-12-01)
// gjør at ArenaService.skalManueltFordeles gir true, dvs. manuell fordeling (AVKLAR_FORDELING).
private fun kantIKantArenaSak() = TestArenaSak(
    saknummer = "ABC-555",
    sakRegistrert = LocalDate.of(2020, 1, 1),
    sisteVedtak = TestArenaVedtak(
        vedtakId = 100,
        fra = LocalDate.of(2020, 1, 1),
        til = LocalDate.of(2021, 3, 1),
        maxdatoOrdinaer = LocalDate.of(2021, 3, 1),
        maxdatoAap = LocalDate.of(2021, 3, 1),
    ),
)

private fun opprettBehandlingManuellFordelingDigitalSøknad(connection: DBConnection) {
    val testPerson = TestPersoner.leggTil { arenaSak = kantIKantArenaSak() }
    val journalpostId = TestJournalposter.leggTil {
        digitalSøknad()
        person = testPerson
    }.journalpostId()

    opprettFordelingsbehandling(
        connection = connection,
        journalpostId = journalpostId,
    )
}

private fun opprettBehandlingManuellFordelingPapirSøknad(connection: DBConnection) {
    val testPerson = TestPersoner.leggTil { arenaSak = kantIKantArenaSak() }
    val journalpostId = TestJournalposter.leggTil {
        papirsøknad()
        person = testPerson
    }.journalpostId()

    opprettFordelingsbehandling(
        connection = connection,
        journalpostId = journalpostId,
    )
}

private fun opprettFordelingsbehandling(
    connection: DBConnection,
    journalpostId: JournalpostId,
) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Fordeling)

    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id)
            .medCallId()
    )
}

private fun opprettBehandlingManuellFordeling(connection: DBConnection) {
    val testPerson = TestPersoner.leggTil { aktivSakIArena = true }
    val journalpostId = TestJournalposter.leggTil {
        person = testPerson
    }.journalpostId()

    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Fordeling)

    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id)
            .medCallId()
    )
}

private fun opprettBehandlingAvklarTeam(connection: DBConnection) {
    val journalpostId = TestJournalposter.leggTil {
        journalførendeEnhet = "4260"
    }.journalpostId()

    val behandlingId = BehandlingRepositoryImpl(connection)
        .opprettBehandling(journalpostId, TypeBehandling.Fordeling)
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id)
            .medCallId()
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

    println("Gå til http://localhost:3000/postmottak/${behandlingRepository.hent(behandlingId).referanse.referanse}/")
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

private fun opprettBehandlingPapirSøknadKategoriser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = TestJournalposter.leggTil {
        papirsøknad()
    }.journalpostId()

    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

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

internal fun postgreSQLContainer(): PostgreSQLContainer<Nothing> {
    val postgres = PostgreSQLContainer<Nothing>("postgres:16")
    postgres.waitingFor(HostPortWaitStrategy().withStartupTimeout(Duration.of(60L, ChronoUnit.SECONDS)))
    postgres.start()
    return postgres
}
