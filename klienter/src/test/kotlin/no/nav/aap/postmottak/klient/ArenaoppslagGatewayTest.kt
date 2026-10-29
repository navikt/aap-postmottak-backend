package no.nav.aap.postmottak.klient

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.arena.ArenaoppslagGatewayImpl
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.modell.TestArenaSak
import no.nav.aap.postmottak.test.modell.TestPersoner
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

@Fakes
class ArenaoppslagGatewayTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }
    }

    val arenaOppslagGatewayFake = ArenaoppslagGatewayImpl()

    @Test
    fun `Kan parse harHistorikk`() {
        val testPerson = TestPersoner.leggTil { harHistorikkIArena = true }

        val res = runBlocking {
            val person = Person(1, UUID.randomUUID(), listOf(testPerson.aktivIdent()))
            arenaOppslagGatewayFake.harHistorikk(person)
        }

        assertThat(res).isEqualTo(true)
    }

    @Test
    fun `Kan parse harSignifikantHistorikk`() {
        val testPerson = TestPersoner.leggTil { harHistorikkIArena = true }

        val vedtak = runBlocking {
            val person = Person(1, UUID.randomUUID(), listOf(testPerson.aktivIdent()))
            arenaOppslagGatewayFake.harSignifikantHistorikk(person, LocalDate.now())
        }

        assertThat(vedtak.saker()).containsExactly(1234)
    }

    @Test
    fun `Kan parse historikkStatusDetalj`() {
        val sak = TestArenaSak(sakId = 4321)
        val testPerson = TestPersoner.leggTil { arenaSak = sak }

        val res = runBlocking {
            val person = Person(1, UUID.randomUUID(), listOf(testPerson.aktivIdent()))
            arenaOppslagGatewayFake.historikkStatusDetalj(person, LocalDate.now())
        }

        assertThat(res).isNotNull
        assertThat(res.sisteSakOgVedtak?.sakId).isEqualTo(sak.sakId)
        assertThat(res.sisteSakOgVedtak?.saknummer).isEqualTo(sak.saknummer)
        assertThat(res.sisteSakOgVedtak?.sisteVedtak?.vedtakId).isEqualTo(sak.sisteVedtak.vedtakId)
        assertThat(res.sisteSakOgVedtak?.sisteVedtak?.maxdatoAap).isEqualTo(sak.sisteVedtak.maxdatoAap)
        assertThat(res.sisteUtbetalingDato).isEqualTo(sak.sisteUtbetalingsdato)
    }

    @Test
    fun `historikkStatusDetalj returnerer tom liste ved ikke funnet`() {
        val testPerson = TestPersoner.leggTil {}

        val res = runBlocking {
            val person = Person(1, UUID.randomUUID(), listOf(testPerson.aktivIdent()))
            arenaOppslagGatewayFake.historikkStatusDetalj(person, LocalDate.now())
        }

        assertThat(res.sisteSakOgVedtak).isNull()
        assertThat(res.sisteUtbetalingDato).isNull()
        assertThat(res.telleverk).isNull()
    }

}
