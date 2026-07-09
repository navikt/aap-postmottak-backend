package no.nav.aap.postmottak

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostStatus
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.fordeler.InnkommendeJournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.AzurePortHolder
import no.nav.aap.postmottak.test.FakeServers
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

// Kjøres opp for å få logback i console uten json
fun main() {
    val postgres = postgreSQLContainer()
    AzurePortHolder.setPort(8071)
    FakeServers().start()

    // Starter server
    embeddedServer(Netty, port = 8070) {
        val dbConfig = DbConfig(
            url = postgres.jdbcUrl,
            username = postgres.username,
            password = postgres.password
        )
        // Useful for connecting to the test database locally
        // jdbc URL contains the host and port and database name.
        println("jdbcUrl: ${postgres.jdbcUrl}. Password: ${postgres.password}. Username: ${postgres.username}.")
        server(
            dbConfig, postgresRepositoryRegistry, defaultGatewayProvider()
        )

        val datasource = initDatasource(dbConfig, SimpleMeterRegistry())

        datasource.transaction {
            opprettBehandlingAvklarTeam(it)
            opprettBehandlingFinnSak(it)
            opprettBehandlingKategoriser(it)
            opprettBehandlingDigitaliser(it)
            opprettBehandlingVurderOpprettelseAvSak(it)
            opprettBehandlingPapirSøknadKategoriser(it)
        }

    }.start(wait = true)
}

private fun opprettBehandlingAvklarTeam(connection: DBConnection) {
    val behandling =
        BehandlingRepositoryImpl(connection).opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)

    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(1, behandling.id).medCallId()
    )
}

/**
 * Løser AVKLAR_TEMA-avklaringsbehovet slik at en seedet behandling kan gå forbi [no.nav.aap.postmottak.forretningsflyt.steg.journalføring.AvklarTemaSteg].
 *
 * Å lagre en temavurdering ([AvklarTemaRepositoryImpl.lagreTemaAvklaring]) løser ikke selve
 * avklaringsbehovet, så uten dette stopper alle seedede behandlinger på "Avklar tema".
 */
private fun løsAvklarTema(connection: DBConnection, behandlingId: BehandlingId) {
    val avklaringsbehovene = Avklaringsbehovene(AvklaringsbehovRepositoryImpl(connection), behandlingId)
    avklaringsbehovene.leggTil(Definisjon.AVKLAR_TEMA, StegType.AVKLAR_TEMA)
    avklaringsbehovene.løsAvklaringsbehov(Definisjon.AVKLAR_TEMA, "Løst av TestApp-seeding", "TESTAPP")
}

private fun opprettBehandlingFinnSak(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(2)
    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    løsAvklarTema(connection, behandlingId)
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
    løsAvklarTema(connection, behandlingId)
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
    løsAvklarTema(connection, behandlingId)
    SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("1010"))
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )

}

/**
 * Seeder en Journalføringsbehandling som stopper på steget "Manuell vurdering av opprettelse av sak i Kelvin".
 *
 * Tema settes til AAP og det seedes et regelresultat med utfall MANUELL, slik at
 * [no.nav.aap.postmottak.forretningsflyt.steg.journalføring.VurderOpprettelseAvSakSteg] krever manuell vurdering.
 */
private fun opprettBehandlingVurderOpprettelseAvSak(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(5)

    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    løsAvklarTema(connection, behandlingId)
    InnkommendeJournalpostRepositoryImpl(connection).lagre(
        InnkommendeJournalpost(
            journalpostId = journalpostId,
            brevkode = null,
            behandlingstema = null,
            status = InnkommendeJournalpostStatus.EVALUERT,
            regelresultat = Regelresultat(
                regelMap = mapOf(
                    "KelvinSakRegel" to true,
                    "ErIkkeReisestønadRegel" to true,
                    "ErIkkeAnkeRegel" to true,
                    "ArenaSakRegel" to false,
                    "TrengerManuellVurderingRegel" to true,
                ),
                forJournalpost = journalpostId.referanse,
            ),
        )
    )
    FlytJobbRepository(connection).leggTil(
        JobbInput(ProsesserBehandlingJobbUtfører)
            .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
    )
}

private fun opprettBehandlingPapirSøknadKategoriser(connection: DBConnection) {
    val behandlingRepository = BehandlingRepositoryImpl(connection)
    val journalpostId = JournalpostId(TestJournalposter.PAPIR_SØKNAD.referanse)

    val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
    AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
    løsAvklarTema(connection, behandlingId)
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