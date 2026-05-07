package no.nav.aap.fordeler.arena

import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.komponenter.gateway.Gateway

interface VeilarbarenaGateway: Gateway {
    fun hentOppfølgingsenhet(personident: String): NavEnhet?
}