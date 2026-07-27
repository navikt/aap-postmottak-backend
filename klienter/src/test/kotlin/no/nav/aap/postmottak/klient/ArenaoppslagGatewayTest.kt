package no.nav.aap.postmottak.klient

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.PersonIkkeFunnetIArenaException
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.arena.ArenaoppslagGatewayImpl
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestIdenter
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
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
        val res = runBlocking {
            val testPerson = Person(1, UUID.randomUUID(), listOf(TestIdenter.IDENT_MED_SAK_I_ARENA))
            arenaOppslagGatewayFake.harHistorikk(testPerson)
        }

        assertThat(res).isEqualTo(true)
    }

    @Test
    fun `Kan parse harSignifikantHistorikk`() {
        val vedtak = runBlocking {
            val testPerson = Person(1, UUID.randomUUID(), listOf(TestIdenter.IDENT_MED_SAK_I_ARENA))
            arenaOppslagGatewayFake.harSignifikantHistorikk(testPerson, LocalDate.now())
        }

        assertThat(vedtak.saker()).containsExactly(1234)
    }

    @Test
    fun `Kan parse MaksdatoResponse`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.sisteVedtakMedMaksdato(TestIdenter.IDENT_MED_SAK_I_ARENA)
        }

        assertThat(res).isNotNull
        assertThat(res?.sakId).isEqualTo(1234)
    }

    @Test
    fun `MaksdatoResponse returnerer tom liste ved ikke funnet`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.sisteVedtakMedMaksdato(TestIdenter.DEFAULT_IDENT)
        }

        assertThat(res).isNull()
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

    @Test
    fun `Kan parse VurderingsgrunnlagResponse`() {
        val res = runBlocking {
            arenaOppslagGatewayFake.hentArenasakForManuellVurdering(TestIdenter.IDENT_MED_SAK_I_ARENA)
        }

        assertThat(res.saksnummer).isEqualTo("ABC-123")
        assertThat(res.erAktiv).isTrue()
        assertThat(res.under52Uker).isTrue()
        assertThat(res.gjenståendeOrdinæreDager).isEqualTo(67)
        assertThat(res.gjenståendeUnntaksDager).isNull()
        assertThat(res.sisteVedtak?.vedtakId).isEqualTo(99)
        assertThat(res.sisteUtbetaling).isEqualTo(LocalDate.parse("2024-05-10"))
    }

    @Test
    fun `Vurderingsgrunnlag kaster PersonIkkeFunnetIArenaException ved 404`() {
        assertThatThrownBy {
            runBlocking {
                arenaOppslagGatewayFake.hentArenasakForManuellVurdering(TestIdenter.DEFAULT_IDENT)
            }
        }.isInstanceOf(PersonIkkeFunnetIArenaException::class.java)
    }
}