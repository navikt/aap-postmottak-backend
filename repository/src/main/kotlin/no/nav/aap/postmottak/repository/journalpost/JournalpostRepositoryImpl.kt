package no.nav.aap.postmottak.repository.journalpost

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.AvsenderMottaker
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Dokument
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Filtype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variant
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl

data class DbDokument(
    val dokumentInfoId: DokumentInfoId,
    val brevkode: String,
    val filtype: Filtype,
    val variantformat: Variantformat,
    val tittel: String?,
) {
    companion object {
        fun fraDokument(dokument: Dokument) = dokument.varianter.map {
            DbDokument(
                dokument.dokumentInfoId,
                dokument.brevkode,
                it.filtype,
                it.variantformat,
                dokument.tittel ?: "Dokument uten tittel"
            )
        }
    }
}

class JournalpostRepositoryImpl(private val connection: DBConnection) : JournalpostRepository {
    private val personRepositoryImpl = PersonRepositoryImpl(connection)

    companion object : Factory<JournalpostRepositoryImpl> {
        override fun konstruer(connection: DBConnection): JournalpostRepositoryImpl {
            return JournalpostRepositoryImpl(connection)
        }
    }

    override fun lagre(journalpost: Journalpost) {
        val query = """
            INSERT INTO JOURNALPOST (JOURNALPOST_ID, JOURNALFORENDE_ENHET, PERSON_ID, STATUS, MOTTATT_DATO, MOTTATT_TID, TEMA, KANAL, SAKSNUMMER, FAGSYSTEM, BEHANDLINGSTEMA, TITTEL)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        val journalpostId = connection.executeReturnKey(query) {
            setParams {
                setLong(1, journalpost.journalpostId.referanse)
                setString(2, journalpost.journalførendeEnhet)
                setLong(3, journalpost.person.id)
                setString(4, journalpost.status.name)
                setLocalDate(5, journalpost.mottattDato)
                setLocalDateTime(6, journalpost.mottattTid)
                setString(7, journalpost.tema)
                setEnumName(8, journalpost.kanal)
                setString(9, journalpost.saksnummer.toString())
                setString(10, journalpost.fagsystem)
                setString(11, journalpost.behandlingstema)
                setString(12, journalpost.tittel)
            }
        }

        val dokumentQuery = """
                INSERT INTO DOKUMENT (JOURNALPOST_ID, DOKUMENT_INFO_ID, BREVKODE, VARIANTFORMAT, FILTYPE, TITTEL) 
                VALUES (?, ?, ?, ?, ?, ?)
            """.trimIndent()
        connection.executeBatch(dokumentQuery, journalpost.dokumenter.flatMap { DbDokument.fraDokument(it) }) {
            setParams { dokument ->
                setLong(1, journalpostId)
                setString(2, dokument.dokumentInfoId.dokumentInfoId)
                setString(3, dokument.brevkode)
                setEnumName(4, dokument.variantformat)
                setEnumName(5, dokument.filtype)
                setString(6, dokument.tittel)
            }
        }

        connection.execute(
            "INSERT INTO AVSENDERMOTTAKER (JOURNALPOST_ID, IDENT, IDENT_TYPE, NAVN) VALUES (?, ?, ?, ?)",
        ) {
            setParams {
                setLong(1, journalpostId)
                setString(2, journalpost.avsenderMottaker?.id)
                setString(3, journalpost.avsenderMottaker?.idType)
                setString(4, journalpost.avsenderMottaker?.navn)
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
            tittel = row.getStringOrNull("TITTEL"),
            mottattDato = row.getLocalDate("MOTTATT_DATO"),
            mottattTid = row.getLocalDateTimeOrNull("MOTTATT_TID"),
            kanal = row.getEnum("KANAL"),
            saksnummer = row.getStringOrNull("SAKSNUMMER"),
            avsenderMottaker = hentAvsenderMottaker(row.getLong("ID")),
            dokumenter = hentDokumenter(row.getLong("ID")),
            fagsystem = row.getStringOrNull("FAGSYSTEM"),
            behandlingstema = row.getStringOrNull("BEHANDLINGSTEMA")
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
                DbDokument(
                    dokumentInfoId = DokumentInfoId(it.getString("DOKUMENT_INFO_ID")),
                    variantformat = it.getEnum("VARIANTFORMAT"),
                    filtype = it.getEnum("FILTYPE"),
                    brevkode = it.getString("BREVKODE"),
                    tittel = it.getStringOrNull("TITTEL"),
                )
            }
        }.groupBy { Dokument(it.dokumentInfoId, it.brevkode, it.tittel, emptyList()) }
            .map { (key, value) -> Dokument(
                key.dokumentInfoId,
                key.brevkode,
                key.tittel,
                value.map { Variant(it.filtype, it.variantformat) })
            }
    }

    private fun hentAvsenderMottaker(journalpostId: Long): AvsenderMottaker {
        val query = "SELECT * FROM AVSENDERMOTTAKER WHERE JOURNALPOST_ID = ?"

        return connection.queryList(query) {
            setParams {
                setLong(1, journalpostId)
            }
            setRowMapper {
                AvsenderMottaker(
                    id = it.getStringOrNull("IDENT"),
                    idType = it.getStringOrNull("IDENT_TYPE"),
                    navn = it.getStringOrNull("NAVN"),
                )
            }
        }.single()
    }
}