package no.nav.aap.postmottak.klient.pdl

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.Adressebeskyttelseskode
import no.nav.aap.postmottak.gateway.GeografiskTilknytningType
import no.nav.aap.postmottak.gateway.Gradering
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

@Fakes
class PdlTest {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }
    }

    @Test
    fun `Kan parse hentPersonBolk`() {
        val test = PdlGraphqlKlient().hentPersonBolk(listOf("1234"))
        assertThat(test?.size).isEqualTo(1)
    }

    @Test
    fun `Kan parse hentPerson`() {
        val test = PdlGraphqlKlient().hentFødselsdato("1234")
        assertNotNull(test)
    }

    @Test
    fun `Kan parse hentIdenter`() {
        val test = PdlGraphqlKlient().hentAlleIdenterForPerson("1234")
        assertNotNull(test)
    }

    @Test
    fun `Kan parse geografiskTilknytning`() {
        val test = PdlGraphqlKlient().hentAdressebeskyttelseOgGeolokasjon(Ident("1234"))
        assertThat(test.adressebeskyttelse).isEqualTo(listOf(Gradering(Adressebeskyttelseskode.UGRADERT)))
        assertThat(test.geografiskTilknytning.gtType).isEqualTo(GeografiskTilknytningType.KOMMUNE)
        assertThat(test.geografiskTilknytning.gtKommune).isEqualTo("3207")
    }
}