package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface AvklaringRepository {
    fun lagreTeamAvklaring(behandlingId: BehandlingId, vurdering: Boolean)
    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: Brevkode)
    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String)
    fun lagreSakVurdeirng(behandlingId: BehandlingId, saksnummer: Saksnummer?)

    fun hentTemaAvklaring(behandlingId: BehandlingId): TemaVurdeirng?
    fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering?
    fun hentSakAvklaring(behandlingId: BehandlingId): Saksvurdering?
    fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering?
}