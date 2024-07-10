package no.nav.aap.httpclient

import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.aap.httpclient.error.DefaultResponseHandler
import no.nav.aap.httpclient.request.GetRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpHeaders
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class RestClientTest {
    @Test
    fun `test at nav-call-id og x-correlation-id blir lagt til på get-kall`() {
        val clientConfig = ClientConfig()
        val tokenProvider = NoTokenTokenProvider()
        val errorHandler = DefaultResponseHandler()

        val (slot, httpClient) = mockHttpClient()

        val client = RestClient(clientConfig, tokenProvider, errorHandler, httpClient)

        val res = client.get<TestClass>(URI.create("http://vg.no"), GetRequest())

        assertThat(res).isEqualTo(TestClass(2))
        assertThat(slot.isCaptured).isTrue()
        assertThat(slot.captured.headers().map().keys).contains("Nav-Call-Id", "X-Correlation-ID")
    }


    private fun mockHttpClient(): Pair<CapturingSlot<HttpRequest>, HttpClient> {
        val httpClient = mockk<HttpClient>()

        val slot = slot<HttpRequest>()
        val responseMock = mockk<HttpResponse<String>>()

        every { responseMock.statusCode() } returns 200
        every { responseMock.body() } returns """{"hei": 2}"""
        every { responseMock.headers() } returns HttpHeaders.of(mapOf()) { _, _ -> true }

        every {
            httpClient.send(
                capture(slot), any<HttpResponse.BodyHandler<String>>()
            )
        } returns responseMock

        return Pair(slot, httpClient)
    }

    data class TestClass(val hei: Int)
}