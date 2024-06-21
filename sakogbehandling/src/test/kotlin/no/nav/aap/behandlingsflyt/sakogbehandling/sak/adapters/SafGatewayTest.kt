package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.net.http.HttpHeaders

class SafGatewayTest {
    @Test
    fun `hente ut filnavn fra header`() {
        val headers =
            HttpHeaders.of(mapOf("Content-Disposition" to listOf("inline; filename=400000000_ARKIV.pdf"))) { _, _ -> true }

        val response = extractFileNameFromHeaders(headers)

        Assertions.assertThat(response).isEqualTo("400000000_ARKIV.pdf")
    }
}