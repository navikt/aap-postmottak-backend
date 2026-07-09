package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface VurderOpprettelseAvSakRepository : Repository {
    fun lagre(behandlingId: BehandlingId, vurdering: VurderOpprettelseAvSakVurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): VurderOpprettelseAvSakVurdering?
}

