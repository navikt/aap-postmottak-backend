package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection

class RegelRepository(private val connection: DBConnection) {

    fun hentRegelresultat(regelId: Long): Regelresultat {
        return connection.queryList(
            """
            SELECT * FROM REGELSETT_RESULTAT rr
            JOIN REGEL_EVALUERING re ON re.REGEL_RESULTAT_ID = rr.ID
            WHERE rr.INNKOMMENDE_JOURNALPOST = ?
        """.trimIndent()
        ) {
            setParams { setLong(1, regelId) }
            setRowMapper { row ->
                row.getString("REGEL_NAVN") to row.getBoolean("RESULTAT")
            }
        }.toMap().let { Regelresultat(it) }
    }

    fun lagre(journalpostId: Long, regelresultat: Regelresultat) {
        val systemNavn = if (regelresultat.skalTilKelvin()) "KELVIN" else "ARENA"

        val regelResultatId = connection.executeReturnKey(
            """
            INSERT INTO REGELSETT_RESULTAT (INNKOMMENDE_JOURNALPOST, SYSTEM_NAVN) VALUES (?, ?)
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId) }
            setParams { setString(2, systemNavn) }
        }

        val regelEvalueringQuery = """
            INSERT INTO REGEL_EVALUERING (REGEL_RESULTAT_ID, REGEL_NAVN, RESULTAT) VALUES (?, ?, ?)
        """.trimIndent()

        connection.executeBatch(
            regelEvalueringQuery, regelresultat.regelMap.entries
        ) {
            setParams { (regelNavn, resultat) ->
                setLong(1, regelResultatId)
                setString(2, regelNavn)
                setBoolean(3, resultat)
            }
        }
    }
}