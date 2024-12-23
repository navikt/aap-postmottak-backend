package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst

class InformasjonskravGrunnlagImpl(private val connection: DBConnection): InformasjonskravGrunnlag {

    override fun oppdaterFaktagrunnlagForKravliste(
        kravliste: List<Informasjonskravkonstruktør>,
        kontekst: FlytKontekst
    ): List<Informasjonskravkonstruktør> {
        // Hva gir dette leddet?
        return kravliste.filterNot { kravtype -> kravtype.konstruer(connection).oppdater(kontekst) == Informasjonskrav.Endret.ENDRET }
    }
}
