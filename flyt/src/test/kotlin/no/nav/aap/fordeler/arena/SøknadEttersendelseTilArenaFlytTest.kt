package no.nav.aap.fordeler.arena

import io.mockk.every
import io.mockk.verify
import no.nav.aap.WithDependencies
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.test.WithMotor
import no.nav.aap.postmottak.test.await
import no.nav.aap.postmottak.test.fakes.SØKNAD_ETTERSENDELSE
import no.nav.aap.postmottak.test.fakes.WithFakes
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate


class SøknadEttersendelseTilArenaFlytTest: WithFakes, WithDependencies, WithMotor {

    companion object {

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            GatewayRegistry.register<PdlKlientSpy>()
                .register<ArenaKlientSpy>()
        }
    }

    @AfterEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE JOBB CASCADE""")
        }
    }

    val dataSource = InitTestDatabase.dataSource

    @Test
    fun `happycase for søknad oppretter sak i arena og journalfører automatsik`() {
        val journalpostId = SØKNAD_ETTERSENDELSE

        val persondataGateway = GatewayProvider.provide(PersondataGateway::class)
        val arenaGateway = GatewayProvider.provide(ArenaGateway::class)

        every { persondataGateway.hentFødselsdato(any()) } returns LocalDate.now().minusYears(70)
        every { arenaGateway.harAktivSak(any()) } returns false

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId))
        }

        await(10000) {
            verify(exactly = 1) { arenaGateway.opprettArenaOppgave(any()) }
        }
    }

}
