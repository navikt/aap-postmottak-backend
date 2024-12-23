package no.nav.aap.postmottak.flyt.steg

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekstMedPerioder

interface BehandlingSteg {

    fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat

    fun vedTilbakeføring(kontekst: FlytKontekst) {

    }
}
