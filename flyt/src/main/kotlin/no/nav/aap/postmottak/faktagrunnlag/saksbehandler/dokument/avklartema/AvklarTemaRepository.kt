package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface AvklarTemaRepository: Repository {
    fun lagreTeamAvklaring(behandlingId: BehandlingId, vurdering: Boolean)
    fun hentTemaAvklaring(behandlingId: BehandlingId): TemaVurdeirng?
}
