package no.nav.aap.behandlingsflyt.faktagrunnlag.dokument

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.verdityper.dokument.JournalpostId

class LazyStrukturertDokument<T : PeriodisertData>(
    private val journalpostId: JournalpostId,
    private val brevkode: Brevkode,
    private val type: Class<T>,
    private val connection: DBConnection
) : StrukturerteData {

    private var cache: StrukturertDokument<T>? = null

    fun hent(): StrukturertDokument<T>? {
        if (cache != null) {
            return cache
        }
        val strukturerteData =
            connection.queryFirstOrNull("SELECT strukturert_dokument FROM MOTTATT_DOKUMENT WHERE journalpost = ?") {
                setParams {
                    setString(1, journalpostId.identifikator)
                }
                setRowMapper {
                    it.getStringOrNull("strukturert_dokument")
                }
            }
        if (strukturerteData == null) {
            return null
        }
        cache = StrukturertDokument(DefaultJsonMapper.fromJson(strukturerteData, type), brevkode)

        return cache
    }


}