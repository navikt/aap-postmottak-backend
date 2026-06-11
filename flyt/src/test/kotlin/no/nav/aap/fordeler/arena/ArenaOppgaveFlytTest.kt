package no.nav.aap.fordeler.arena

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.spyk
import io.mockk.verify
import no.nav.aap.WithDependencies
import no.nav.aap.WithDependencies.Companion.repositoryRegistry
import no.nav.aap.arenaoppslag.kontrakt.apiv1.ArenaVedtak
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SakMedSisteVedtakOgMaksdato
import no.nav.aap.arenaoppslag.kontrakt.apiv1.SignifikantHistorikkResponse
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.ArenaoppslagGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.arena.ArenaWebservicesGatewayImpl
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
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
            register<ArenaoppslagGatewaySpy>()
            register<ArenaWebservicesGatewaySpy>()
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

        val arenaoppslagGateway = gatewayProvider.provide(ArenaoppslagGateway::class)
        val arenaWebservicesGateway = gatewayProvider.provide(ArenaWebservicesGateway::class)
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)

        clearAllMocks()
        coEvery { arenaoppslagGateway.harHistorikk(any()) } returns true
        coEvery { arenaoppslagGateway.harSignifikantHistorikk(any(), any()) } returns SignifikantHistorikkResponse(true,
            listOf(ArenaVedtak(
                1, "AKTIV", null, null, null, "AAP", null
            )))

        every { arenaWebservicesGateway.harAktivSak(any()) } returns false
        every { unleashGateway.isEnabled(PostmottakFeature.BegrensetFordelingTilKelvin, any()) } returns true

        dataSource.transaction { connection ->
            val behandlingId = BehandlingRepositoryImpl(connection)
                .opprettBehandling(journalpostId, TypeBehandling.Fordeling)
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }

        util.ventPåSvar()
        verify {
            unleashGateway.isEnabled(
                withArg {
                    assertThat(it).isEqualTo(PostmottakFeature.BegrensetFordelingTilKelvin)
                },
                any()
            )
        }
        verify(exactly = 1) {
            arenaWebservicesGateway.opprettArenaOppgave(withArg {
                assertThat(it.oppgaveType).isEqualTo(ArenaOppgaveType.STARTVEDTAK)
            })
        }
    }

    @Test
    fun `happycase for søknad oppretter sak i arena og journalfører automatisk`() {
        val journalpostId = TestJournalposter.SØKNAD_ETTERSENDELSE

        val arenaoppslagGateway = gatewayProvider.provide(ArenaoppslagGateway::class)
        val arenaWebservicesGateway = gatewayProvider.provide(ArenaWebservicesGateway::class)
        val unleashGateway = gatewayProvider.provide(UnleashGateway::class)

        clearAllMocks()
        coEvery { arenaoppslagGateway.harHistorikk(any()) } returns true
        coEvery { arenaoppslagGateway.harSignifikantHistorikk(any(), any()) } returns SignifikantHistorikkResponse(true,
            listOf(ArenaVedtak(
                1, "AKTIV", null, null, null, "AAP", null
            )))
        every { arenaWebservicesGateway.harAktivSak(any()) } returns false
        every { unleashGateway.isEnabled(PostmottakFeature.BegrensetFordelingTilKelvin, any()) } returns true

        dataSource.transaction { connection ->
            val behandlingId = BehandlingRepositoryImpl(connection)
                .opprettBehandling(journalpostId, TypeBehandling.Fordeling)
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }
        util.ventPåSvar()

        verify {
            unleashGateway.isEnabled(
                withArg {
                    assertThat(it).isEqualTo(PostmottakFeature.BegrensetFordelingTilKelvin)
                },
                any()
            )
        }
        verify(exactly = 1) {
            arenaWebservicesGateway.opprettArenaOppgave(withArg {
                assertThat(it.oppgaveType).isEqualTo(ArenaOppgaveType.BEHENVPERSON)
            })
        }
    }

}

class ArenaoppslagGatewaySpy : ArenaoppslagGateway {
    companion object : Factory<ArenaoppslagGatewaySpy> {
        val klient = spyk(ArenaoppslagGatewaySpy())
        override fun konstruer() = klient
    }

    override suspend fun harHistorikk(person: Person): Boolean {
        TODO("kalles ikke på")
    }

    override suspend fun harSignifikantHistorikk(
        person: Person,
        mottattDato: LocalDate
    ): SignifikantHistorikkResponse {
        TODO("kalles ikke på")
    }

    override suspend fun maksdatoForSaker(ident: Ident): List<SakMedSisteVedtakOgMaksdato> {
        TODO("kalles ikke på")
    }

    override suspend fun sisteUtbetalingsdatoForPerson(ident: Ident): LocalDate? {
        TODO("kalles ikke på")
    }

}

class ArenaWebservicesGatewaySpy : ArenaWebservicesGateway {

    companion object : Factory<ArenaWebservicesGatewayImpl> {
        val klient = spyk(ArenaWebservicesGatewayImpl.konstruer())
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

class UnleashSpy : UnleashGateway {
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
