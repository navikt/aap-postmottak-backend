package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

class InnkommendeJournalpostRepositoryImpl(
    private val connection: DBConnection,
    private val regelRepository: RegelRepositoryImpl = RegelRepositoryImpl(connection)
) : InnkommendeJournalpostRepository {

    companion object : Factory<InnkommendeJournalpostRepositoryImpl> {
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

    override fun hentId(journalpostId: JournalpostId): Long {
        return connection.queryFirst(
            """
            SELECT id FROM innkommende_journalpost WHERE journalpost_id = ?
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { row -> row.getInt("ID").toLong() }
        }
    }

    override fun hent(journalpostId: JournalpostId): InnkommendeJournalpost {
        return connection.queryFirst(
            """
            SELECT * FROM innkommende_journalpost WHERE journalpost_id = ?
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper{row -> innkommendeJournalpostRowMapper(row, journalpostId)}
        }
    }

    override fun hentHvisEksisterer(journalpostId: JournalpostId): InnkommendeJournalpost? {
        return connection.queryFirstOrNull(
            """
            SELECT * FROM innkommende_journalpost WHERE journalpost_id = ?
        """.trimIndent()
        ) {
            setParams { setLong(1, journalpostId.referanse) }
            setRowMapper { row -> innkommendeJournalpostRowMapper(row, journalpostId) }
        }
    }

    private fun innkommendeJournalpostRowMapper(row: Row, journalpostId: JournalpostId): InnkommendeJournalpost {
        return InnkommendeJournalpost(
            journalpostId = JournalpostId(row.getLong("journalpost_id")),
            status = row.getEnum("status"),
            behandlingstema = row.getStringOrNull("behandlingstema"),
            brevkode = row.getStringOrNull("brevkode"),
            årsakTilStatus = row.getEnumOrNull("aarsak_til_status"),
            enhet = row.getStringOrNull("enhet"),
            regelresultat = regelRepository.hentRegelresultat(row.getLong("ID"), journalpostId),
            brukerId = row.getStringOrNull("bruker_id"),
        )
    }

    override fun lagre(innkommendeJournalpost: InnkommendeJournalpost): Long {
        val query = """
            INSERT INTO innkommende_journalpost (journalpost_id, status, aarsak_til_status, behandlingstema, brevkode, enhet, bruker_id) 
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val id = connection.executeReturnKey(query) {
            setParams {
                setLong(1, innkommendeJournalpost.journalpostId.referanse)
                setEnumName(2, innkommendeJournalpost.status)
                setEnumName(3, innkommendeJournalpost.årsakTilStatus)
                setString(4, innkommendeJournalpost.behandlingstema)
                setString(5, innkommendeJournalpost.brevkode)
                setString(6, innkommendeJournalpost.enhet)
                setString(7, innkommendeJournalpost.brukerId)
            }
        }
        if (innkommendeJournalpost.regelresultat != null) {
            regelRepository.lagre(id, innkommendeJournalpost.regelresultat!!)
        }
        return id
    }

    override fun update(innkommendeJournalpost: InnkommendeJournalpost) {
        val id = hentId(innkommendeJournalpost.journalpostId)
        connection.execute(
            """
            UPDATE innkommende_journalpost
            SET status = ?, aarsak_til_status = ?, behandlingstema = ?, brevkode = ?, enhet = ?, bruker_id = ?
            WHERE id = ?
            """.trimIndent()
        ) {
            setParams {
                setEnumName(1, innkommendeJournalpost.status)
                setEnumName(2, innkommendeJournalpost.årsakTilStatus)
                setString(3, innkommendeJournalpost.behandlingstema)
                setString(4, innkommendeJournalpost.brevkode)
                setString(5, innkommendeJournalpost.enhet)
                setString(6, innkommendeJournalpost.brukerId)
                setLong(7, id)
            }
            setResultValidator { require(it == 1) { "Forventet å oppdatere en rad, oppdaterte $it" } }
        }
        val regelresultat = innkommendeJournalpost.regelresultat
        if (regelresultat != null) {
            regelRepository.slett(id)
            regelRepository.lagre(id, regelresultat)
        }
    }

    override fun finn(ident: Ident): List<InnkommendeJournalpost> {
        return connection.queryList(
            """
            SELECT * FROM innkommende_journalpost WHERE bruker_id = ?
        """.trimIndent()
        ) {
            setParams { setString(1, ident.identifikator) }
            setRowMapper { row ->
                val journalpostId = JournalpostId(row.getLong("journalpost_id"))
                innkommendeJournalpostRowMapper(row, journalpostId)
            }
        }
    }
}
