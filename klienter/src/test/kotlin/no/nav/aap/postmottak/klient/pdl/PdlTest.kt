package no.nav.aap.postmottak.klient.pdl

import WithFakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PdlTest: WithFakes {

    @Test
    fun `Kan parse hentPersonBolk`() {
        val test = PdlGraphQLClient.withClientCredentialsRestClient().hentPersonBolk(listOf("1234"))

        assertThat(test?.size).isEqualTo(1)
    }
}