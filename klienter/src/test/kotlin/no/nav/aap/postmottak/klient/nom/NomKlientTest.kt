package no.nav.aap.postmottak.klient.nom

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.modell.TestPersoner
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
        val testPerson = TestPersoner.leggTil {
            skjermet = true
        }
        val actual = client.erEgenAnsatt(testPerson.aktivIdent())

        assertThat(actual).isTrue()
    }

    @Test
    fun erIkkeEgenansatt() {
        val client = NomKlient()

        val testPerson = TestPersoner.leggTil {
            skjermet = false
        }

        val actual = client.erEgenAnsatt(testPerson.aktivIdent())

        assertThat(actual).isFalse()
    }

}