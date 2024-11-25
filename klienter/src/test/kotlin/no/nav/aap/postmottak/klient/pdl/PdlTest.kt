package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class PdlTest: WithFakes {

    @Test
    fun `Kan parse hentPersonBolk`() {
        val test = PdlGraphQLClient.withClientCredentialsRestClient().hentPersonBolk(listOf("1234"))

        assertThat(test?.size).isEqualTo(1)
    }

    @Test
    fun `Kan parse hentPerson`() {
        val test = PdlGraphQLClient.withClientCredentialsRestClient().hentPerson("1234")
        assertNotNull(test)
    }

    @Test
    fun `Kan parse hentIdenter`() {
        val test = PdlGraphQLClient.withClientCredentialsRestClient().hentAlleIdenterForPerson("1234")
        assertNotNull(test)
    }

    @Test
    fun `Kan parse geografiskTilknytning`() {
        val test = PdlGraphQLClient.withClientCredentialsRestClient().hentAdressebeskyttelseOgGeolokasjon(Ident("1234"))
        assertThat(test.hentPerson?.adressebeskyttelse).isEqualTo(listOf(Adressebeskyttelseskode.UGRADERT))
        assertThat(test.hentGeografiskTilknytning?.gtType).isEqualTo(GeografiskTilknytningType.KOMMUNE)
        assertThat(test.hentGeografiskTilknytning?.gtKommune).isEqualTo("3207")
    }
}