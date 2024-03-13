package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import java.time.LocalDateTime

class MottattDokument(
    val journalpostId: JournalpostId,
    val sakId: SakId,
    val behandlingId: BehandlingId?,
    val mottattTidspunkt: LocalDateTime,
    val type: Brevkode,
    val status: Status = Status.MOTTATT,
    private val strukturertDokument: StrukturerteData?
) {

    fun <T : PeriodisertData> strukturerteData(): StrukturertDokument<T>? {
        if (strukturertDokument == null) {
            return null
        }
        if (strukturertDokument is LazyStrukturertDokument<*>) {
            return strukturertDokument.hent() as StrukturertDokument<T>?
        }
        return strukturertDokument as StrukturertDokument<T>?
    }

    fun ustrukturerteData(): String? {
        if (strukturertDokument == null) {
            return null
        }

        val data = data()

        return DefaultJsonMapper.toJson(data!!.data)
    }

    private fun data(): StrukturertDokument<*>? {
        return if (strukturertDokument is LazyStrukturertDokument<*>) {
            strukturertDokument.hent()
        } else {
            strukturertDokument as StrukturertDokument<*>?
        }
    }
}