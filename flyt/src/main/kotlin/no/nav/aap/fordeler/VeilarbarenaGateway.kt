package no.nav.aap.fordeler

import no.nav.aap.komponenter.gateway.Gateway

interface VeilarbarenaGateway: Gateway {
    fun hentOppfølgingsenhet(personident: String): NavEnhet?
}