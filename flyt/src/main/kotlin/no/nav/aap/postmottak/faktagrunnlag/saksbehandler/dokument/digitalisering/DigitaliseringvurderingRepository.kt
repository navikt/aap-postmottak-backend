package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface DigitaliseringsvurderingRepository: Repository {
    fun lagre(behandlingId: BehandlingId, strukturertDokument: Digitaliseringsvurdering)
    fun hentHvisEksisterer(behandlingId: BehandlingId): Digitaliseringsvurdering?
}