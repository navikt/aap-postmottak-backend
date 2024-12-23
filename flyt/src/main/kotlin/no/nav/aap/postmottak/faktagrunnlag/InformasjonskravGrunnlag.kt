package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

interface InformasjonskravGrunnlag {

    fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør>
}
