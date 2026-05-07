package no.nav.aap.postmottak.klient.arena

import no.nav.aap.komponenter.config.requiredConfigForKey

data class ArenaoppslagConfig(
    val proxyBaseUrl: String = requiredConfigForKey("ARENAOPPSLAG_PROXY_BASE_URL"),
    val scope: String = requiredConfigForKey("ARENAOPPSLAG_SCOPE")
)
