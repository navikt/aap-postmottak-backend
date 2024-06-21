package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.httpclient.tokenprovider.OidcToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.net.http.HttpHeaders

class SafGatewayTest {
    companion object {
        private val fakes = Fakes()

        @JvmStatic
        @AfterAll
        fun afterAll() {
            fakes.close()
        }
    }

    @Test
    @Disabled
    fun `hente dokumentoversikt fra saf`() {
        val gateway = SafGateway

        // TODO: Generer fake token
        val token = OidcToken(
            ""
        )

        val documents = gateway.hentDokumenterForSak(Saksnummer("123"), token)
        assertThat(documents).hasSize(1)
    }

    @Test
    fun `hente ut filnavn fra header`() {
        val headers =
            HttpHeaders.of(mapOf("Content-Disposition" to listOf("inline; filename=400000000_ARKIV.pdf"))) { _, _ -> true }

        val response = extractFileNameFromHeaders(headers)

        assertThat(response).isEqualTo("400000000_ARKIV.pdf")
    }
}