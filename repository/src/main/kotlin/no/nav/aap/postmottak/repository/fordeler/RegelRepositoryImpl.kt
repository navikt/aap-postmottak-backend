package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory

class RegelRepositoryImpl(private val connection: DBConnection): RegelRepository {

    companion object: Factory<RegelRepositoryImpl> {
        override fun konstruer(connection: DBConnection): RegelRepositoryImpl {
            return RegelRepositoryImpl(connection)
        }
    }
    
    override fun hentRegelresultat(journalpostId: Long): Regelresultat {
        return connection.queryList(
            """
            SELECT * FROM REGELSETT_RESULTAT rr
            JOIN REGEL_EVALUERING re ON re.REGEL_RESULTAT_ID = rr.ID
            JOIN INNKOMMENDE_JOURNALPOST ijp ON ijp.ID = rr.INNKOMMENDE_JOURNALPOST
            WHERE ijp.JOURNALPOST_ID = ?
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId) }
            setRowMapper { row ->
                row.getString("REGEL_NAVN") to row.getBoolean("RESULTAT")
            }
        }.toMap().let { Regelresultat(it) }
    }

    override fun lagre(journalpostId: Long, regelresultat: Regelresultat) {
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