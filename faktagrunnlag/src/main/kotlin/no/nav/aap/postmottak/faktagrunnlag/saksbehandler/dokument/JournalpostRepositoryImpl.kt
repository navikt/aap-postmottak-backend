package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.postmottak.klient.joark.Dokument
import no.nav.aap.postmottak.klient.joark.DokumentInfoId
import no.nav.aap.postmottak.klient.joark.Ident
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.klient.joark.JournalpostStatus
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface JournalpostRepository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost?
    fun lagre(journalpost: Journalpost, behandlingId: BehandlingId)
}
class JournalpostRepositoryImpl(private val connection: DBConnection): JournalpostRepository {

    override fun lagre(journalpost: Journalpost, behandlingId: BehandlingId) {
        val personIdent = if (journalpost is Journalpost.MedIdent && journalpost.personident is Ident.Personident) journalpost.personident.id else null
        val aktørIdent = if (journalpost is Journalpost.MedIdent && journalpost.personident is Ident.Aktørid) journalpost.personident.id else null
        val query = """
            INSERT INTO JOURNALPOST (JOURNALPOST_ID, BEHANDLING_ID, JOURNALFORENDE_ENHET, PERSON_IDENT, AKTOER_IDENT, STATUS, MOTTATT_DATO, TEMA) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val journalpostId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, journalpost.journalpostId.referanse)
                setLong(2, behandlingId.toLong())
                setString(3, journalpost.journalførendeEnhet)
                setString(4, personIdent)
                setString(5, aktørIdent)
                setString(6, journalpost.status().name)
                setLocalDate(7, journalpost.mottattDato())
                setString(8, journalpost.tema)
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
            SELECT * FROM JOURNALPOST WHERE BEHANDLING_ID = ?
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

    private fun mapJournalpost(row: Row): Journalpost {
        val personIdent = row.getStringOrNull("PERSON_IDENT")
        val aktørIdent = row.getStringOrNull("AKTOER_IDENT")
        return if (personIdent != null || aktørIdent != null) {
            Journalpost.MedIdent(
                personident = if (personIdent != null) Ident.Personident(personIdent) else Ident.Aktørid(aktørIdent!!),
                journalpostId = JournalpostId(row.getLong("JOURNALPOST_ID")),
                journalførendeEnhet = row.getStringOrNull("JOURNALFORENDE_ENHET"),
                status = JournalpostStatus.valueOf(row.getString("STATUS")),
                tema = row.getString("TEMA"),
                mottattDato = row.getLocalDate("MOTTATT_DATO"),
                dokumenter = hentDokumenter(row.getLong("ID"))
            )
        } else {
             Journalpost.UtenIdent(
                journalpostId = JournalpostId(row.getLong("JOURNALPOST_ID")),
                journalførendeEnhet = row.getStringOrNull("JOURNALFORENDE_ENHET"),
                status = JournalpostStatus.valueOf(row.getString("STATUS")),
                tema = row.getString("TEMA"),
                mottattDato = row.getLocalDate("MOTTATT_DATO"),
                dokumenter = hentDokumenter(row.getLong("ID"))
            )
        }
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