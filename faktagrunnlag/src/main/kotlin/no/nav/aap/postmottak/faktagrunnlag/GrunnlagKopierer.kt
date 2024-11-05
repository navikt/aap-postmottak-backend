package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class GrunnlagKopierer(connection: DBConnection) {

    private val avklarTemaRepository = AvklarTemaRepository(connection)
    private val avklarSaksnummerRepository = SaksnummerRepository(connection)
    private val kategorivurderingRepository = KategorivurderingRepository(connection)
    private val struktureringsvurderingRepository = StruktureringsvurderingRepository(connection)

    fun overfør(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        require(fraBehandlingId != tilBehandlingId)

        avklarTemaRepository.kopier(fraBehandlingId, tilBehandlingId)
        avklarSaksnummerRepository.kopier(fraBehandlingId, tilBehandlingId)
        kategorivurderingRepository.kopier(fraBehandlingId, tilBehandlingId)
        struktureringsvurderingRepository.kopier(fraBehandlingId, tilBehandlingId)
    }
}
