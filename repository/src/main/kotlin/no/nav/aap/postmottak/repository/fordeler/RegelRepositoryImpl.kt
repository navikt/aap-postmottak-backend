package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl

class RegelRepositoryImpl(private val connection: DBConnection) : RegelRepository {
    private val personRepositoryImpl = PersonRepositoryImpl(connection)

    companion object : Factory<RegelRepositoryImpl> {
        override fun konstruer(connection: DBConnection): RegelRepositoryImpl {
            return RegelRepositoryImpl(connection)
        }
    }

    override fun hentRegelresultat(journalpostId: JournalpostId): Regelresultat? {
        return connection.queryList(
            """
                SELECT * FROM REGELSETT_RESULTAT rr
                JOIN REGEL_EVALUERING re ON re.REGEL_RESULTAT_ID = rr.ID
                JOIN INNKOMMENDE_JOURNALPOST ijp ON ijp.ID = rr.INNKOMMENDE_JOURNALPOST
                WHERE ijp.JOURNALPOST_ID = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { row ->
                row.getString("REGEL_NAVN") to row.getBoolean("RESULTAT")
            }
        }.let {
            if (it.isEmpty()) null else Regelresultat(it.toMap())
        }
    }

    override fun hentRegelresultat(innkommendeJournalpostId: Long): Regelresultat? {
        return connection.queryList(
            """
                SELECT * FROM REGELSETT_RESULTAT rr
                JOIN REGEL_EVALUERING re ON re.REGEL_RESULTAT_ID = rr.ID
                WHERE innkommende_journalpost = ?
            """.trimIndent()
        ) {
            setParams { setLong(1, innkommendeJournalpostId) }
            setRowMapper { row ->
                row.getString("REGEL_NAVN") to row.getBoolean("RESULTAT")
            }
        }.let {
            if (it.isEmpty()) null else Regelresultat(it.toMap())
        }
    }


    override fun hentPersonerMedJournalpostVideresendtTilKelvin(): List<Person> {
        return connection.queryList(
            """
            SELECT distinct person_id FROM JOURNALPOST j 
            INNER JOIN INNKOMMENDE_JOURNALPOST ijp
            ON j.JOURNALPOST_ID = ijp.JOURNALPOST_ID
            JOIN REGELSETT_RESULTAT rr ON rr.INNKOMMENDE_JOURNALPOST = ijp.ID
            WHERE rr.SYSTEM_NAVN = 'KELVIN'
        """.trimIndent()
        ) {
            setRowMapper { row -> personRepositoryImpl.hent(row.getLong("PERSON_ID")) }
        }
    }

    override fun lagre(innkommendeJournalpostId: Long, regelresultat: Regelresultat) {
        val systemNavn = if (regelresultat.skalTilKelvin()) "KELVIN" else "ARENA"

        val regelResultatId = connection.executeReturnKey(
            """
            INSERT INTO REGELSETT_RESULTAT (INNKOMMENDE_JOURNALPOST, SYSTEM_NAVN) VALUES (?, ?)
        """.trimIndent()
        ) {
            setParams { setLong(1, innkommendeJournalpostId) }
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