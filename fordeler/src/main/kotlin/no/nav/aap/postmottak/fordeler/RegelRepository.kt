package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection

class RegelRepository(private val connection: DBConnection) {

    fun hentRegelresultat(regelId: Long): RegelMap {
        return connection.queryList("""
            SELECT * FROM REGELSETT_RESULTAT rr
            JOIN REGEL_EVALUERING re ON re.REGEL_RESULTAT_ID == rr.ID
            WHERE rr.VENTENDE_JOURNALPOST_ID = ?
        """.trimIndent()) {
            setParams { setLong(1, regelId) }
            setRowMapper {
                row -> row.getString("REGEL_NAVN") to row.getBoolean("RESULTAT")
            }
        }.toMap()
    }

    //fun lag
}