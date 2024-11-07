package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.postmottak.sakogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.sakogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostStatus
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface JournalpostRepository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost?
    fun hentHvisEksisterer(journalpostId: JournalpostId): Journalpost?
    fun lagre(journalpost: Journalpost)
}

class JournalpostRepositoryImpl(private val connection: DBConnection) : JournalpostRepository {
    private val personRepository = PersonRepository(connection)

    override fun lagre(journalpost: Journalpost) {
        val query = """
            INSERT INTO JOURNALPOST (JOURNALPOST_ID, JOURNALFORENDE_ENHET, PERSON_ID, STATUS, MOTTATT_DATO, TEMA) VALUES (?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val journalpostId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, journalpost.journalpostId.referanse)
                setString(2, journalpost.journalførendeEnhet)
                setLong(3, journalpost.person.id)
                setString(4, journalpost.status().name)
                setLocalDate(5, journalpost.mottattDato())
                setString(6, journalpost.tema)
            }
        }

        val dokumentQuery = """
                INSERT INTO DOKUMENT (JOURNALPOST_ID, DOKUMENT_INFO_ID, BREVKODE, VARIANTFORMAT, FILTYPE) VALUES (?, ?, ?, ?, ?)
            """.trimIndent()
        connection.executeBatch(dokumentQuery, journalpost.dokumenter()) {
            setParams { dokument ->
                setLong(1, journalpostId)
                setString(2, dokument.dokumentInfoId.dokumentInfoId)
                setString(3, dokument.brevkode)
                setEnumName(4, dokument.variantFormat)
                setEnumName(5, dokument.filtype)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost? {
        val query = """
            SELECT journalpost.* FROM JOURNALPOST 
            JOIN behandling on behandling.journalpost_id = journalpost.journalpost_id 
            WHERE behandling.id = ? ORDER BY OPPRETTET_TID DESC LIMIT 1
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapJournalpost(it)
            }
        }
    }

    override fun hentHvisEksisterer(journalpostId: JournalpostId): Journalpost? {
        val query = """
            SELECT * FROM JOURNALPOST WHERE journalpost_id = ? ORDER BY OPPRETTET_TID DESC LIMIT 1
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setLong(1, journalpostId.referanse)
            }
            setRowMapper {
                mapJournalpost(it)
            }
        }
    }

    private fun mapJournalpost(row: Row): Journalpost {
        return Journalpost(
            person = personRepository.hent(row.getLong("PERSON_ID")),
            journalpostId = JournalpostId(row.getLong("JOURNALPOST_ID")),
            journalførendeEnhet = row.getStringOrNull("JOURNALFORENDE_ENHET"),
            status = JournalpostStatus.valueOf(row.getString("STATUS")),
            tema = row.getString("TEMA"),
            mottattDato = row.getLocalDate("MOTTATT_DATO"),
            dokumenter = hentDokumenter(row.getLong("ID"))
        )
    }

    private fun hentDokumenter(journalpostId: Long): List<Dokument> {
        val query = """
            SELECT * FROM DOKUMENT WHERE JOURNALPOST_ID = ?
        """.trimIndent()
        return connection.queryList(query) {
            setParams {
                setLong(1, journalpostId)
            }
            setRowMapper {
                Dokument(
                    dokumentInfoId = DokumentInfoId(it.getString("DOKUMENT_INFO_ID")),
                    variantFormat = it.getEnum("VARIANTFORMAT"),
                    filtype = it.getEnum("FILTYPE"),
                    brevkode = it.getStringOrNull("BREVKODE"),
                )
            }
        }
    }
}