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
    
    override fun hent(journalpostId: JournalpostId): InnkommendeJournalpost {
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
    
    override fun lagre(innkommendeJournalpost: InnkommendeJournalpost) {
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
