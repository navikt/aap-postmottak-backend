package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface AvklaringRepository {
    fun lagreTeamAvklaring(behandlingId: BehandlingId, vurdering: Boolean)
    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: Brevkode)
    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)
    fun lagreSakVurdering(behandlingId: BehandlingId, saksvurdering: Saksvurdering)

    fun hentTemaAvklaring(behandlingId: BehandlingId): TemaVurdeirng?
    fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering?
    fun hentSakAvklaring(behandlingId: BehandlingId): Saksvurdering?
    fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering?
}