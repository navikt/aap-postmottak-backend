package no.nav.aap.postmottak.repository.journalpost

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl

class JournalpostRepositoryImpl(private val connection: DBConnection) : JournalpostRepository {
    private val personRepositoryImpl = PersonRepositoryImpl(connection)

    companion object: Factory<JournalpostRepositoryImpl> {
        override fun konstruer(connection: DBConnection): JournalpostRepositoryImpl {
            return JournalpostRepositoryImpl(connection)
        }   
    }
    
    override fun lagre(journalpost: Journalpost) {
        val query = """
            INSERT INTO JOURNALPOST (JOURNALPOST_ID, JOURNALFORENDE_ENHET, PERSON_ID, STATUS, MOTTATT_DATO, TEMA, KANAL) VALUES (?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val journalpostId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, journalpost.journalpostId.referanse)
                setString(2, journalpost.journalførendeEnhet)
                setLong(3, journalpost.person.id)
                setString(4, journalpost.status().name)
                setLocalDate(5, journalpost.mottattDato())
                setString(6, journalpost.tema)
                setEnumName(7, journalpost.kanal)
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

    override fun hentHvisEksisterer(behandlingsreferanse: Behandlingsreferanse): Journalpost? {
        val query = """
            SELECT journalpost.* FROM JOURNALPOST 
            JOIN behandling on behandling.journalpost_id = journalpost.journalpost_id 
            WHERE behandling.referanse = ? ORDER BY OPPRETTET_TID DESC LIMIT 1
        """.trimIndent()
        return connection.queryFirstOrNull(query) {
            setParams {
                setUUID(1, behandlingsreferanse.referanse)
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
            person = personRepositoryImpl.hent(row.getLong("PERSON_ID")),
            journalpostId = JournalpostId(row.getLong("JOURNALPOST_ID")),
            journalførendeEnhet = row.getStringOrNull("JOURNALFORENDE_ENHET"),
            status = Journalstatus.valueOf(row.getString("STATUS")),
            tema = row.getString("TEMA"),
            mottattDato = row.getLocalDate("MOTTATT_DATO"),
            kanal = row.getEnum("KANAL"),
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
                    brevkode = it.getString("BREVKODE"),
                )
            }
        }
    }
}