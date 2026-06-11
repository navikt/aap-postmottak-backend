package no.nav.aap.fordeler.arena

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface AvklarFordelingRepository: Repository {
    fun hentVurderingHvisEksisterer(behandlingId: BehandlingId): AvklarFordelingVurdering?
    fun lagreVurdering(behandlingId: BehandlingId, vurdering: AvklarFordelingVurdering)
}