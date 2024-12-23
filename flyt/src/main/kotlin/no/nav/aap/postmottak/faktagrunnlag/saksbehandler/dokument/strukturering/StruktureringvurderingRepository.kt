package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface StruktureringsvurderingRepository: Repository {
    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)
    fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering?
}