package no.nav.aap.fordeler

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.gateway.Oppgavetype

interface NorgGateway : Gateway {
    fun hentAktiveEnheter(): List<NavEnhet>
    fun finnArbeidsfordelingsEnhet(
        geografiskTilknytning: String?,
        erNavansatt: Boolean,
        diskresjonskode: Diskresjonskode,
        behandlingstema: String,
        behandlingstype: String? = null,
        oppgavetype: Oppgavetype? = null
    ): NavEnhet?
}

enum class Diskresjonskode { SPFO, SPSF, ANY }