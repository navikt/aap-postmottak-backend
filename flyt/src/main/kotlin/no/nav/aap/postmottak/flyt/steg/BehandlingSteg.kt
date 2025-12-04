package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

interface BehandlingSteg {

    fun utfør(kontekst: FlytKontekst): StegResultat

}
