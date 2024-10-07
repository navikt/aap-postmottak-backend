package no.nav.aap.behandlingsflyt.forretningsflyt.gjenopptak

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class GjenopptakRepository(private val connection: DBConnection) {

    fun finnBehandlingerForGjennopptak(): List<BehandlingId> {
        val query = """
            SELECT b.id, b.sak_id 
            FROM BEHANDLING b
             JOIN AVKLARINGSBEHOV a ON a.behandling_id = b.id
             JOIN (
                SELECT DISTINCT ON (AVKLARINGSBEHOV_ID) *
                FROM AVKLARINGSBEHOV_ENDRING
                ORDER BY AVKLARINGSBEHOV_ID, OPPRETTET_TID DESC
             ) ae ON ae.AVKLARINGSBEHOV_ID = a.id
            WHERE b.STATUS = '${Status.UTREDES.name}' 
            AND ae.status = ?
            AND ae.frist <= CURRENT_DATE
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setEnumName(1, Status.OPPRETTET)
            }
            setRowMapper { row ->
                BehandlingId(row.getLong("id"))
            }
        }
    }
}