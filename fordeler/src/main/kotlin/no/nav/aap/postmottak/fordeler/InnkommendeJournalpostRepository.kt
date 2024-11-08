package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class InnkommendeJournalpostRepository(
    private val connection: DBConnection,
    private val regelRepository: RegelRepository = RegelRepository(connection)
) {
    fun hent(journalpostId: JournalpostId): InnkommendeJournalpost {
        return connection.queryFirst("""
            SELECT * FROM innkommende_journalpost WHERE journalpost_id = ?
        """.trimIndent()){
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { row -> InnkommendeJournalpost(
                journalpostId = journalpostId,
                status = row.getEnum("status"),
                regelresultat = regelRepository.hentRegelresultat(row.getLong("ID"))
            ) }
        }
    }
    
    fun lagre(innkommendeJournalpost: InnkommendeJournalpost) {
        val query = """
            INSERT INTO innkommende_journalpost (journalpost_id, status) VALUES (?, ?)
        """.trimIndent()
        val journalpostId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, innkommendeJournalpost.journalpostId.referanse)
                setEnumName(2, innkommendeJournalpost.status)
            }
        }
        
        regelRepository.lagre(journalpostId, innkommendeJournalpost.regelresultat)
    }

}
