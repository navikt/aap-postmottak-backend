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

    val apiIntern = ArenaoppslagGatewayImpl()

    @Test
    fun `Kan parse PersonEksistererIAAPArena`() {
        val res = runBlocking {
            apiIntern.harAapSakIArena(Person(1, UUID.randomUUID(), listOf(TestIdenter.IDENT_MED_SAK_I_ARENA)))
        }

        assertThat(res).isEqualTo(true)
    }
}