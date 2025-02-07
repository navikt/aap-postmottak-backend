package no.nav.aap.fordeler

import no.nav.aap.lookup.gateway.Gateway

interface VeilarbarenaGateway: Gateway {
    fun hentOppfølgingsenhet(personident: String): NavEnhet?
}