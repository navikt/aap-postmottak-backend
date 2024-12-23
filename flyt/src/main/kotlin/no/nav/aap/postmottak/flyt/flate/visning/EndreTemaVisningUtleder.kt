package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

class EndreTemaVisningUtleder(connection: DBConnection): StegGruppeVisningUtleder {
    private val repositoryProvider = RepositoryProvider(connection)
    private val avklarTemaRepository = repositoryProvider.provide(AvklarTemaRepository::class)
    
    override fun skalVises(behandlingId: BehandlingId): Boolean {
        val avklaring = avklarTemaRepository.hentTemaAvklaring(behandlingId)
        return avklaring?.skalTilAap == false
    }

    override fun gruppe(): StegGruppe {
        return StegGruppe.ENDRE_TEMA
    }
}