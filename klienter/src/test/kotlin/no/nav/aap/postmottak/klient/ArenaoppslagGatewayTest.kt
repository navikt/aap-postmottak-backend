package no.nav.aap.postmottak.klient

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.arena.ArenaoppslagGatewayImpl
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestIdenter
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
    fun `Kan parse PersonEksistererIAAPArena`() {
        val res = runBlocking {
            val testPerson = Person(1, UUID.randomUUID(), listOf(TestIdenter.IDENT_MED_SAK_I_ARENA))
            arenaOppslagGatewayFake.harAapSakIArena(testPerson)
        }

        assertThat(res).isEqualTo(true)
    }

    @Test
    fun `Kan parse SignifikanteSakerResponse`() {
        val res = runBlocking {
            val testPerson = Person(1, UUID.randomUUID(), listOf(TestIdenter.IDENT_MED_SAK_I_ARENA))
            arenaOppslagGatewayFake.hentSakerMedSignifikantHistorikk(testPerson, LocalDate.now())
        }

        assertThat(res).containsExactly(1234)
    }

    @Test
    fun `Kan parse MaksdatoResponse`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.maksdatoForSaker(TestIdenter.IDENT_MED_SAK_I_ARENA)
        }

        assertThat(res).hasSize(1)
        assertThat(res.first().sakId).isEqualTo(1234)
    }

    @Test
    fun `MaksdatoResponse returnerer tom liste ved ikke funnet`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.maksdatoForSaker(TestIdenter.DEFAULT_IDENT)
        }

        assertThat(res).isEmpty()
    }

    @Test
    fun `Kan parse SisteUtbetalingerResponse`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.sisteUtbetalingsdatoForPerson(TestIdenter.IDENT_MED_SAK_I_ARENA)
        }

        assertThat(res).isEqualTo(LocalDate.parse("2024-05-10"))
    }

    @Test
    fun `SisteUtbetalingerResponse returnerer null ved ikke funnet`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.sisteUtbetalingsdatoForPerson(TestIdenter.DEFAULT_IDENT)
        }

        assertThat(res).isNull()
    }
}