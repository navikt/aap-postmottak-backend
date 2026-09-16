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
    fun `Kan parse MaksdatoResponse`() {
        val testPerson = TestPersoner.leggTil { arenaSak = TestArenaSak() }

        val res = runBlocking {
            arenaOppslagGatewayFake.sisteVedtakMedMaksdato(testPerson.aktivIdent())
        }

        assertThat(res).isNotNull
        assertThat(res?.sakId).isEqualTo(1234)
    }

    @Test
    fun `MaksdatoResponse returnerer tom liste ved ikke funnet`() {
        val testPerson = TestPersoner.leggTil {}

        val res = runBlocking {
            arenaOppslagGatewayFake.sisteVedtakMedMaksdato(testPerson.aktivIdent())
        }

        assertThat(res).isNull()
    }

    @Test
    fun `Kan parse SisteUtbetalingerResponse`() {
        val testPerson = TestPersoner.leggTil {
            arenaSak = TestArenaSak(sisteUtbetalingsdato = LocalDate.parse("2024-05-10"))
        }

        val res = runBlocking {
            arenaOppslagGatewayFake.sisteUtbetalingsdatoForPerson(testPerson.aktivIdent())
        }

        assertThat(res).isEqualTo(LocalDate.parse("2024-05-10"))
    }

    @Test
    fun `SisteUtbetalingerResponse returnerer null ved ikke funnet`() {
        val testPerson = TestPersoner.leggTil {}

        val res = runBlocking {
            arenaOppslagGatewayFake.sisteUtbetalingsdatoForPerson(testPerson.aktivIdent())
        }

        assertThat(res).isNull()
    }
}
