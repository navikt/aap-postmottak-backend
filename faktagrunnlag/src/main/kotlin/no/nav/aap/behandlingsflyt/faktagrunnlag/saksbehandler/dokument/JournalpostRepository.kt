package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Dokument
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.adapters.saf.JournalpostStatus
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.verdityper.dokument.DokumentInfoId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class JournalpostRepository(private val connection: DBConnection) {
    fun lagre(journalpost: Journalpost, behandlingId: BehandlingId) {
        val personIdent = if (journalpost is Journalpost.MedIdent) journalpost.personident.id else null
        val aktørIdent = if (journalpost is Journalpost.MedIdent) journalpost.personident.id else null
        val query = """
            INSERT INTO JOURNALPOST (JOURNALPOST_ID, BEHANDLING_ID, JOURNALFORENDE_ENHET, PERSON_IDENT, AKTOER_IDENT, STATUS, MOTTATT_DATO) VALUES (?, ?, ?, ?, ?, ?, ?)
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
            }
        }


        val dokumentQuery = """
                INSERT INTO DOKUMENT (JOURNALPOST_ID, DOKUMENT_INFO_ID, BREVKODE, VARIANTFORMAT, TITTEL, FILTYPE) VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        connection.executeBatch(dokumentQuery, journalpost.dokumenter()) {
            setParams { dokument ->
                setLong(1, journalpostId)
                setString(2, dokument.dokumentInfoId.dokumentInfoId)
                setString(3, dokument.brevkode)
                setEnumName(4, dokument.variantFormat)
                setString(5, dokument.tittel)
                setEnumName(6, dokument.filtype)
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost? {
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
        if (personIdent != null || aktørIdent != null) {
            return Journalpost.MedIdent(
                personident = if (personIdent != null) Ident.Personident(personIdent) else Ident.Aktørid(aktørIdent!!),
                journalpostId = JournalpostId(row.getLong("JOURNALPOST_ID")),
                journalførendeEnhet = row.getString("JOURNALFORENDE_ENHET"),
                status = JournalpostStatus.valueOf(row.getString("STATUS")),
                mottattDato = row.getLocalDate("MOTTATT_DATO"),
                dokumenter = hentDokumenter(row.getLong("JOURNALPOST_ID"))
            )
        } else {
            return Journalpost.UtenIdent(
                journalpostId = JournalpostId(row.getLong("JOURNALPOST_ID")),
                journalførendeEnhet = row.getString("JOURNALFORENDE_ENHET"),
                status = JournalpostStatus.valueOf(row.getString("STATUS")),
                mottattDato = row.getLocalDate("MOTTATT_DATO"),
                dokumenter = hentDokumenter(row.getLong("JOURNALPOST_ID"))
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
                    tittel = it.getString("TITTEL")
                )
            }
        }
    }
}