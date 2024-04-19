package no.nav.aap.httpclient.request

import no.nav.aap.httpclient.Header
import java.time.Duration

class GetRequest(
    private val additionalHeaders: List<Header> = emptyList(),
    private val timeout: Duration = Duration.ofSeconds(60),
) : Request {
    override fun additionalHeaders(): List<Header> {
        return additionalHeaders
    }

    override fun timeout(): Duration {
        return timeout
    }
}