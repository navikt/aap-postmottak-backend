package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class InnkommendeJournalpostRepositoryImpl(
    private val connection: DBConnection,
    private val regelRepository: RegelRepositoryImpl = RegelRepositoryImpl(connection)
): InnkommendeJournalpostRepository {
    
    companion object: Factory<InnkommendeJournalpostRepositoryImpl> {
        override fun konstruer(connection: DBConnection): InnkommendeJournalpostRepositoryImpl {
            return InnkommendeJournalpostRepositoryImpl(connection)
        }
    }
    
    override fun eksisterer(journalpostId: JournalpostId): Boolean {
        return connection.queryFirstOrNull(
            """
            SELECT id FROM innkommende_journalpost WHERE journalpost_id = ?
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { row -> row.getInt("ID") }
        } != null
    }
    
    override fun hent(journalpostId: JournalpostId): InnkommendeJournalpost {
        return connection.queryFirst("""
            SELECT * FROM innkommende_journalpost WHERE journalpost_id = ?
        """.trimIndent()){
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { row -> InnkommendeJournalpost(
                journalpostId = journalpostId,
                status = row.getEnum("status"),
                behandlingstema = row.getStringOrNull("behandlingstema"),
                brevkode = row.getStringOrNull("brevkode"),
                årsakTilStatus = row.getEnumOrNull("aarsak_til_status"),
                enhet = row.getStringOrNull("enhet"),
                regelresultat = regelRepository.hentRegelresultat(row.getLong("ID"))
            ) }
        }
    }
    
    override fun lagre(innkommendeJournalpost: InnkommendeJournalpost): Long {
        val query = """
            INSERT INTO innkommende_journalpost (journalpost_id, status, aarsak_til_status, behandlingstema, brevkode, enhet) 
            VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val id = connection.executeReturnKey(query) {
            setParams {
                setLong(1, innkommendeJournalpost.journalpostId.referanse)
                setEnumName(2, innkommendeJournalpost.status)
                setEnumName(3, innkommendeJournalpost.årsakTilStatus)
                setString(4, innkommendeJournalpost.behandlingstema)
                setString(5, innkommendeJournalpost.brevkode)
                setString(6, innkommendeJournalpost.enhet)
            }
        }
        if (innkommendeJournalpost.regelresultat != null) {
            regelRepository.lagre(id, innkommendeJournalpost.regelresultat!!)
        }
        return id
    }

}
