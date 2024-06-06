package no.nav.aap.behandlingsflyt.flyt.internals

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.motor.JobbStatus
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId

class TestJobbRepository(private val connection: DBConnection) {
    fun harOppgaver(sakId: SakId?, behandlingId: BehandlingId?): Boolean {

        var query = "SELECT count(1) as antall " +
                "FROM JOBB " +
                "WHERE status not in ('${JobbStatus.FERDIG.name}', '${JobbStatus.FEILET.name}')"

        var params = HashMap<Int, Long>()

        if (sakId != null) {
            query += " AND sak_id = ?"
            params[1] = sakId.toLong()
        }
        if (behandlingId != null) {
            query += " AND behandling_id = ?"
            val index = if (sakId != null) {
                2
            } else {
                1
            }
            params[index] = behandlingId.toLong()
        }


        val antall =
            connection.queryFirst(
                query
            ) {
                setParams {
                    for (param in params) {
                        setLong(param.key, param.value)
                    }
                }
                setRowMapper {
                    it.getLong("antall") > 0
                }
            }
        return antall
    }
}
