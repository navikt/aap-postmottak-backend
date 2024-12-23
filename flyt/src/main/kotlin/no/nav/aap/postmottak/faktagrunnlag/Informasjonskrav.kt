package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

interface Informasjonskrav {
    enum class Endret {
        ENDRET,
        IKKE_ENDRET,
    }
    
    fun oppdater(kontekst: FlytKontekst): Endret
}
