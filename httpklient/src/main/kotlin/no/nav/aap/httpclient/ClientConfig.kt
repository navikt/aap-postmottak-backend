package no.nav.aap.httpclient

import java.time.Duration


class ClientConfig(
    val scope: String? = null,
    val connectionTimeout: Duration = Duration.ofSeconds(15),
    val parseableHttpStatuses: List<Int> = emptyList(),
    val additionalHeaders: List<Header> = emptyList(),
    val additionalFunctionalHeaders: List<FunctionalHeader> = emptyList()
) {

    fun isParseableStatus(status: Int): Boolean {
        return parseableHttpStatuses.contains(status)
    }
}