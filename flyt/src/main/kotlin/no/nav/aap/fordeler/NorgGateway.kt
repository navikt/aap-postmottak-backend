package no.nav.aap.fordeler

import no.nav.aap.lookup.gateway.Gateway

interface NorgGateway: Gateway {
    fun hentAktiveEnheter(): List<NavEnhet>
    fun finnEnhet(geografiskTilknytning: String?, erNavansatt: Boolean, diskresjonskode: Diskresjonskode): NavEnhet
}

enum class Diskresjonskode { SPFO, SPSF, ANY }