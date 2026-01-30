package no.nav.aap.fordeler.arena

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.FakeUnleash
import no.nav.aap.WithDependencies
import no.nav.aap.WithDependencies.Companion.repositoryRegistry
import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SignifikanteSakerResponse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.klient.unleash.UnleashService
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.time.LocalDate


@Fakes
@Execution(ExecutionMode.SAME_THREAD)
class ArenaOppgaveFlytTest : WithDependencies {
    companion object {
        private val gatewayProvider = defaultGatewayProvider {
            register<ApiInternSpy>()
            register<ArenaKlientSpy>()
            register<UnleashSpy>()
        }

        private lateinit var dataSource: TestDataSource
        private lateinit var motor: Motor
        private lateinit var util: TestUtil

        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

            dataSource = TestDataSource()
            motor = Motor(
                dataSource,
                2,
                repositoryRegistry = repositoryRegistry,
                gatewayProvider = gatewayProvider,
                jobber = ProsesseringsJobber.alle()
            )
            motor.start()

            util = TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron != null }.map { it.type })
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            motor.stop()
            dataSource.close()
        }

    }

    @Test
    fun `happycase for søknad, oppretter sak i arena og journalfører automatisk`() {
        val journalpostId = TestJournalposter.PERSON_UTEN_SAK_I_BEHANDLINGSFLYT

        val apiInternGateway = gatewayProvider.provide(AapInternApiGateway::class)
        val arenaGateway = gatewayProvider.provide(ArenaGateway::class)
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)

        clearAllMocks()
        every { apiInternGateway.harAapSakIArena(any()) } returns PersonEksistererIAAPArena(eksisterer = true)
        every { apiInternGateway.harSignifikantHistorikkIAAPArena(any(), any()) } returns SignifikanteSakerResponse(
            true,
            listOf("1234")
        )
        every { arenaGateway.harAktivSak(any()) } returns false
        every { unleashGateway.isEnabled(PostmottakFeature.AktiverSignifikantArenaHistorikkRegel, any()) } returns true

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId)
            )
        }

        util.ventPåSvar()

        verify {
            unleashGateway.isEnabled(
                withArg {
                    assertThat(it).isEqualTo(PostmottakFeature.AktiverSignifikantArenaHistorikkRegel)
                },
                any())
        }

        verify(exactly = 1) {
            arenaGateway.opprettArenaOppgave(withArg {
                assertThat(it.oppgaveType).isEqualTo(ArenaOppgaveType.STARTVEDTAK)
            })
        }
    }

    @Test
    fun `happycase for søknad oppretter sak i arena og journalfører automatisk`() {
        val journalpostId = TestJournalposter.SØKNAD_ETTERSENDELSE

        val apiInternGateway = gatewayProvider.provide(AapInternApiGateway::class)
        val arenaGateway = gatewayProvider.provide(ArenaGateway::class)
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)

        clearAllMocks()
        every { apiInternGateway.harAapSakIArena(any()) } returns PersonEksistererIAAPArena(eksisterer = true)
        every { apiInternGateway.harSignifikantHistorikkIAAPArena(any(), any()) } returns
                SignifikanteSakerResponse(harSignifikantHistorikk = true, signifikanteSaker = listOf("1234"))
        every { arenaGateway.harAktivSak(any()) } returns false
        every { unleashGateway.isEnabled(PostmottakFeature.AktiverSignifikantArenaHistorikkRegel, any()) } returns true

        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(1).medJournalpostId(journalpostId)
            )
        }
        util.ventPåSvar()

        verify {
            unleashGateway.isEnabled(
                withArg {
                    assertThat(it).isEqualTo(PostmottakFeature.AktiverSignifikantArenaHistorikkRegel)
                },
                any())
        }

        verify(exactly = 1) {
            arenaGateway.opprettArenaOppgave(withArg {
                assertThat(it.oppgaveType).isEqualTo(ArenaOppgaveType.BEHENVPERSON)
            })
        }
    }

}

class ApiInternSpy : AapInternApiGateway {
    companion object : Factory<ApiInternSpy> {
        val klient = spyk(ApiInternSpy())
        override fun konstruer() = klient
    }

    override fun harAapSakIArena(person: Person): PersonEksistererIAAPArena {
        TODO("kalles ikke på")
    }

    override fun harSignifikantHistorikkIAAPArena(
        person: Person,
        mottattDato: LocalDate
    ): SignifikanteSakerResponse {
        TODO("kalles ikke på")
    }

}

class ArenaKlientSpy : ArenaGateway {

    companion object : Factory<ArenaKlient> {
        val klient = spyk(ArenaKlient.konstruer())
        override fun konstruer() = klient
    }

    override fun nyesteAktiveSak(ident: Ident): String {
        TODO("Not yet implemented")
    }

    override fun harAktivSak(ident: Ident): Boolean {
        TODO("Not yet implemented")
    }

    override fun opprettArenaOppgave(arenaOpprettetForespørsel: ArenaOpprettOppgaveForespørsel): ArenaOpprettOppgaveRespons {
        TODO("Not yet implemented")
    }

    override fun behandleKjoerelisteOgOpprettOppgave(journalpostId: JournalpostId): String {
        TODO("Not yet implemented")
    }

}

class UnleashSpy: UnleashGateway{
    override fun isEnabled(featureToggle: FeatureToggle): Boolean {
        TODO("Not yet implemented")
    }

    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean {
        TODO("Not yet implemented")
    }
    companion object : Factory<UnleashSpy> {
        val klient = spyk(UnleashSpy())
        override fun konstruer() = klient
    }
}
