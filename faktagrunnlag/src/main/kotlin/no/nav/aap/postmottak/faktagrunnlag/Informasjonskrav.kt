package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.verdityper.flyt.FlytKontekst

interface Informasjonskrav {
    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }
    
    fun oppdater(kontekst: FlytKontekst): Endret
}
