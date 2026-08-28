package no.nav.aap.fordeler.arena

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.WithDependencies
import no.nav.aap.WithDependencies.Companion.repositoryRegistry
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.FssOppgaver
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.modell.TestArenaSak
import no.nav.aap.postmottak.test.modell.TestArenaVedtak
import no.nav.aap.postmottak.test.modell.TestPersoner
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
            register<FakeUnleashGateway>()
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
        val testPerson = TestPersoner.leggTil {
            aktivSakIArena = false
            harHistorikkIArena = true
        }

        val identifikator = testPerson.aktivIdent()
        val journalpostId = TestJournalposter.leggTil {
            person = testPerson
            digitalSøknad()
        }.journalpostId()

        val unleashGateway = gatewayProvider.provide(UnleashGateway::class) as FakeUnleashGateway

        unleashGateway.reset()

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

        assertThat(FssOppgaver.oppgaver[identifikator]).hasSize(1)
        assertThat(FssOppgaver.oppgaver[identifikator]!!.single().oppgaveType).isEqualTo(ArenaOppgaveType.STARTVEDTAK)
    }

    @Test
    fun `happycase for søknad oppretter sak i arena og journalfører automatisk`() {
        val testPerson = TestPersoner.leggTil {
            aktivSakIArena = true
            harHistorikkIArena = true
        }
        val identifikator = testPerson.aktivIdent()
        // STANDARD_ETTERSENDING sendes til OppprettOppgaveIArenaJobbUtfører, som alltid oppretter
        // en BEHENVPERSON-oppgave (i motsetning til digital søknad, som sjekker harAktivSak).
        val journalpostId = TestJournalposter.leggTil {
            person = testPerson
            brevkode = Brevkoder.STANDARD_ETTERSENDING
        }.journalpostId()

        val unleashGateway = gatewayProvider.provide(UnleashGateway::class) as FakeUnleashGateway

        unleashGateway.reset()

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

        assertThat(FssOppgaver.oppgaver[identifikator]).hasSize(1)
        assertThat(FssOppgaver.oppgaver[identifikator]!!.single().oppgaveType).isEqualTo(ArenaOppgaveType.BEHENVPERSON)
    }

    @Test
    fun `søknad som er kant-i-kant med Arena-sak klassifiseres til manuell vurdering og stopper på avklar fordeling`() {
        // Kant-i-kant: løpende vedtak (vedtaktypeKode "O") med maxdatoAap innen 20 uker etter
        // journalpostens mottattDato (DATO_REGISTRERT = 2020-12-01) -> ArenaService.skalManueltFordeles gir true.
        val testPerson = TestPersoner.leggTil {
            harHistorikkIArena = true
            arenaSak = TestArenaSak(
                sakId = 1234,
                saknummer = "2024-23456",
                sakStatus = "AKTIV",
                sakRegistrert = LocalDate.of(2024, 1, 1),
                sakAvsluttet = null,
                sisteVedtak = TestArenaVedtak(
                    vedtakId = 99,
                    aktfaseKode = "AKT",
                    vedtaktypeKode = "O",
                    fra = LocalDate.of(2024, 1, 1),
                    til = LocalDate.of(2024, 12, 31),
                    maxdatoOrdinaer = LocalDate.of(2025, 1, 1),
                    maxdatoUnntak = null,
                    maxdatoAap = LocalDate.of(2021, 3, 1),
                ),
            )
        }
        val identifikator = testPerson.aktivIdent()
        val journalpostId =
            TestJournalposter.leggTil {
                person = testPerson
                digitalSøknad()
            }.journalpostId()

        val unleashGateway = gatewayProvider.provide(UnleashGateway::class) as FakeUnleashGateway
        unleashGateway.reset()

        val behandlingId = dataSource.transaction { connection ->
            val id = BehandlingRepositoryImpl(connection)
                .opprettBehandling(journalpostId, TypeBehandling.Fordeling)
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, id.id)
                    .medCallId()
            )
            id
        }

        util.ventPåSvar()

        dataSource.transaction { connection ->
            val behandling = BehandlingRepositoryImpl(connection).hent(behandlingId)
            // Behandlingen står parkert på avklar fordeling-steget
            assertThat(behandling.aktivtSteg()).isEqualTo(StegType.AVKLAR_FORDELING)

            // ...med et åpent AVKLAR_FORDELING-avklaringsbehov til saksbehandler
            val behov = AvklaringsbehovRepositoryImpl(connection)
                .hentAvklaringsbehovene(behandling.id)
                .hentBehovForDefinisjon(Definisjon.AVKLAR_FORDELING)
            assertThat(behov).isNotNull
            assertThat(behov!!.status()).isEqualTo(Status.OPPRETTET)
        }

        // Behandlingen er verken rutet til Arena eller Kelvin – den venter på manuell vurdering
        assertThat(FssOppgaver.oppgaver[identifikator]).isNullOrEmpty()
    }

}

class FakeUnleashGateway : UnleashGateway {
    companion object : Factory<FakeUnleashGateway> {
        private val instance = FakeUnleashGateway()
        override fun konstruer() = instance
    }

    var enabledFeatures: MutableSet<FeatureToggle> = mutableSetOf(
        PostmottakFeature.BegrensetFordelingTilKelvin,
        PostmottakFeature.PostmottakManuellVurdering,
    )

    fun reset() {
        enabledFeatures = mutableSetOf(
            PostmottakFeature.BegrensetFordelingTilKelvin,
            PostmottakFeature.PostmottakManuellVurdering,
        )
    }

    override fun isEnabled(featureToggle: FeatureToggle): Boolean = featureToggle in enabledFeatures

    override fun isEnabled(featureToggle: FeatureToggle, userId: String): Boolean = featureToggle in enabledFeatures
}
