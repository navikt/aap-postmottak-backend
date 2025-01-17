package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface AvklarTemaRepository: Repository {
    fun lagreTemaAvklaring(behandlingId: BehandlingId, vurdering: Boolean, tema: Tema)
    fun hentTemaAvklaring(behandlingId: BehandlingId): TemaVurdering?
}
