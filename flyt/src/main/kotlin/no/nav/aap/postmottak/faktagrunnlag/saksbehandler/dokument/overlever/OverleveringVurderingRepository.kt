package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface OverleveringVurderingRepository : Repository {
    fun lagre(behandlingId: BehandlingId, overleveringVurdering: OverleveringVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): OverleveringVurdering?
}