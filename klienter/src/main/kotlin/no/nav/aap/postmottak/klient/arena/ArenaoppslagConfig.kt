package no.nav.aap.postmottak.klient.arena

import no.nav.aap.komponenter.config.requiredConfigForKey

data class ArenaoppslagConfig(
    val proxyBaseUrl: String = requiredConfigForKey("integrasjon.aap.arenaoppslag.proxy.url"),
    val scope: String = requiredConfigForKey("integrasjon.aap.arenaoppslag.scope")
)
