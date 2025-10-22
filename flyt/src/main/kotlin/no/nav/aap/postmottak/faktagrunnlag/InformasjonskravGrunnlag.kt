package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.steg.StegType

interface InformasjonskravGrunnlag {
    /**
     * @param kravkonstruktører En liste med [Informasjonskrav] som skal oppdateres.
     * @param kontekst Den gjeldende flytkonteksten.
     * @return En liste over informasjonskrav som har endret seg siden forrige kall.
     */
    fun oppdaterFaktagrunnlagForKravliste(
        kravkonstruktører: List<Pair<StegType, Informasjonskravkonstruktør>>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør>
}
