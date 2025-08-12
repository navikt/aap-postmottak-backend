package no.nav.aap.postmottak.klient.nom

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.SKJERMET_IDENT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@Fakes
class NomKlientTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }
    }

    @Test
    fun erEgenAnsatt() {
        val client = NomKlient()
        val actual = client.erEgenAnsatt(SKJERMET_IDENT)

        assertThat(actual).isTrue()
    }

    @Test
    fun erIkkeEgenansatt() {
        val client = NomKlient()

        val actual = client.erEgenAnsatt(Ident("123412341243"))

        assertThat(actual).isFalse()
    }

}