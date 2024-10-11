package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface AvklaringRepository {
    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)

    fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering?
}