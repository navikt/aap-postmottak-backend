package no.nav.aap.postmottak.klient.pdl

import WithFakes
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
}