package no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer

import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.Saksinfo
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SaksnummerRepository(private val connection: DBConnection) {

    fun hentSaksnummre(behandlingId: BehandlingId): List<Saksinfo> =
        connection.queryList("""
            SELECT * FROM (
                SELECT * FROM INNHENTEDE_SAKER_FOR_BEHANDLING WHERE BEHANDLING_ID = ?
                ORDER BY OPPRETTET DESC LIMIT 1) sakinfo
            JOIN SAKER_PAA_BEHANDLING ON INNHENTEDE_SAKER_FOR_BEHANDLING_ID = sakinfo.ID

        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                Saksinfo(row.getString("saksnummer"),
                    row.getPeriode("periode")) }
        }


    fun lagreSaksnummer(behandlingId: BehandlingId, saksinfo: List<Saksinfo>) {
        val id = connection.executeReturnKey("""
            INSERT INTO INNHENTEDE_SAKER_FOR_BEHANDLING (BEHANDLING_ID) VALUES (?)
        """.trimIndent()) { setParams { setLong(1, behandlingId.id) }}

        connection.executeBatch("""
            INSERT INTO SAKER_PAA_BEHANDLING (
                INNHENTEDE_SAKER_FOR_BEHANDLING_ID,
                SAKSNUMMER, 
                PERIODE) 
                VALUES (?, ?, ?::daterange) 
        """.trimIndent(), saksinfo) {
            setParams {
                setLong(1, id)
                setString(2, it.saksnummer)
                setPeriode(3, it.periode)
            }
        }
    }

}
