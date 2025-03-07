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
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.SØKNAD_ETTERSENDELSE
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SøknadEttersendelseTilArenaFlytTest : WithDependencies {

    companion object {
        private val motor = Motor(InitTestDatabase.dataSource, 2, jobber = ProsesseringsJobber.alle())
        val dataSource = InitTestDatabase.dataSource
        val util =
            TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron() != null }.map { it.type() })

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            GatewayRegistry.register<PdlKlientSpy>()
                .register<ArenaKlientSpy>()

            motor.start()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            motor.stop()
        }
    }

    @Test
    fun `happycase for søknad oppretter sak i arena og journalfører automatsik`() {
        val journalpostId = SØKNAD_ETTERSENDELSE

        val persondataGateway = GatewayProvider.provide(PersondataGateway::class)
        val arenaGateway = GatewayProvider.provide(ArenaGateway::class)

        every { persondataGateway.hentFødselsdato(any()) } returns LocalDate.now().minusYears(70)
        every { arenaGateway.harAktivSak(any()) } returns false

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId)
            )
        }
        util.ventPåSvar()
        verify(exactly = 1) { arenaGateway.opprettArenaOppgave(any()) }

    }

}
