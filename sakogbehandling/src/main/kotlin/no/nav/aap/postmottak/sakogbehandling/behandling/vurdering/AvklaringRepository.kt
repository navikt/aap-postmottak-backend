package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface AvklaringRepository {
    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: Brevkode)
    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)

    fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering?
    fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering?
}