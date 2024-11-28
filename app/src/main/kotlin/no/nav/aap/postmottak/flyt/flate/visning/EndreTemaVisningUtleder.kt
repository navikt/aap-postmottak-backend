package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class EndreTemaVisningUtleder(connection: DBConnection): StegGruppeVisningUtleder {
    private val avklarTemaRepository = AvklarTemaRepository(connection)
    
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val avklaring = avklarTemaRepository.hentTemaAvklaring(behandlingId)
        return avklaring?.skalTilAap == false
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.ENDRE_TEMA
    }
}