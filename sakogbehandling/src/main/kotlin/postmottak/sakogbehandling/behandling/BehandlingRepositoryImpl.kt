package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.postmottak.kontrakt.journalpost.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository, BehandlingFlytRepository,
    VurderingRepository {

    override fun opprettBehandling(journalpostId: JournalpostId): Behandling {

        val query = """
            INSERT INTO BEHANDLING (status, type, journalpost_id)
                 VALUES (?, ?, ?)
            """.trimIndent()
        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setEnumName(1, Status.OPPRETTET)
                setString(2, TypeBehandling.DokumentHåndtering.identifikator())
                setLong(3, journalpostId.referanse)
            }
        }

        val behandling = Behandling(
            id = BehandlingId(behandlingId),
            journalpostId = journalpostId,
            versjon = 0,
            vurderinger = Vurderinger()
        )

        return behandling
    }

    override fun lagreTeamAvklaring(behandlingId: BehandlingId, vurdering: Boolean) {
        connection.execute(
            """
            INSERT INTO SKAL_TIL_AAP_AVKLARING (BEHANDLING_ID, SKAL_TIL_AAP) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setBoolean(2, vurdering)
            }
        }
    }

    override fun lagreKategoriseringVurdering(behandlingId: BehandlingId, brevkode: Brevkode) {
        connection.execute(
            """
            INSERT INTO KATEGORIAVKLARING (BEHANDLING_ID, KATEGORI) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, brevkode)
            }
        }
    }

    override fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String) {
        connection.execute(
            """
            INSERT INTO DIGITALISERINGSAVKLARING (BEHANDLING_ID, STRUKTURERT_DOKUMENT) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, strukturertDokument)
            }
        }
    }

    override fun lagreSakVurdeirng(behandlingId: BehandlingId, saksnummer: Saksnummer?) {
        connection.execute(
            """
            INSERT INTO SAKSNUMMER_AVKLARING (BEHANDLING_ID, SAKSNUMMER, OPPRETT_NY) VALUES (
            ?, ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, saksnummer?.toString())
                setBoolean(3, saksnummer == null)
            }
        }
    }

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            journalpostId = JournalpostId(row.getLong("journalpost_id")),
            status = row.getEnum("status"),
            stegHistorikk = hentStegHistorikk(behandlingId),
            versjon = row.getLong("versjon"),
            vurderinger = Vurderinger(
                row.getBooleanOrNull("skal_til_aap")?.let(::Vurdering),
                row.getEnumOrNull<Brevkode, _>("kategori")?.let(::Vurdering),
                row.getStringOrNull("strukturert_dokument")?.let(::Vurdering),
                row.getBooleanOrNull("OPPRETT_NY")?.let {Saksvurdering(
                    row.getStringOrNull("SAKSNUMMER"),
                    it
                ).let(::Vurdering) }
            )
        )
    }

    override fun oppdaterBehandlingStatus(
        behandlingId: BehandlingId,
        status: Status
    ) {
        val query = """UPDATE behandling SET status = ? WHERE ID = ?"""

        return connection.execute(query) {
            setParams {
                setEnumName(1, status)
                setLong(2, behandlingId.toLong())
            }
            setResultValidator {
                require(it == 1)
            }
        }
    }

    override fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand) {
        val updateQuery = """
            UPDATE STEG_HISTORIKK set aktiv = false WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        connection.execute(updateQuery) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
        }

        val query = """
                INSERT INTO STEG_HISTORIKK (behandling_id, steg, status, aktiv, opprettet_tid) 
                VALUES (?, ?, ?, ?, ?)
                """.trimIndent()

        connection.execute(query) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, tilstand.steg())
                setEnumName(3, tilstand.status())
                setBoolean(4, true)
                setLocalDateTime(5, LocalDateTime.now())
            }
        }
    }

    private fun hentStegHistorikk(behandlingId: BehandlingId): List<StegTilstand> {
        val query = """
            SELECT * FROM STEG_HISTORIKK WHERE behandling_id = ? and aktiv = true
        """.trimIndent()

        return connection.queryList(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                StegTilstand(
                    tidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                    stegType = row.getEnum("steg"),
                    stegStatus = row.getEnum("status")
                )
            }
        }
    }

    override fun hent(behandlingId: BehandlingId): Behandling {

        val query = """
            SELECT * FROM BEHANDLING b 
            ${vurderingJoinQuery("SKAL_TIL_AAP_AVKLARING")}
            ${vurderingJoinQuery("KATEGORIAVKLARING")}
            ${vurderingJoinQuery("DIGITALISERINGSAVKLARING")}
            ${vurderingJoinQuery("SAKSNUMMER_AVKLARING")}
            WHERE b.id = ?
            FOR UPDATE OF B
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    fun hentBehandlingType(behandlingId: BehandlingId): TypeBehandling {
        val query = """
            SELECT type FROM BEHANDLING WHERE id = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, behandlingId.toLong())
            }
            setRowMapper { row ->
                TypeBehandling.from(row.getString("type"))
            }
        }
    }

    override fun hent(journalpostId: JournalpostId): Behandling {
        val query = """
            SELECT * FROM BEHANDLING b
            ${vurderingJoinQuery("SKAL_TIL_AAP_AVKLARING")}
            ${vurderingJoinQuery("KATEGORIAVKLARING")}
            ${vurderingJoinQuery("DIGITALISERINGSAVKLARING")}
            ${vurderingJoinQuery("SAKSNUMMER_AVKLARING")}
            WHERE journalpost_id = ?
            FOR UPDATE OF b
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setLong(1, journalpostId.referanse)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    private fun vurderingJoinQuery(tableName: String) =
        """LEFT JOIN $tableName ON $tableName.id = 
            (SELECT id FROM $tableName 
            |WHERE $tableName.behandling_id = b.id 
            |ORDER BY TIDSSTEMPEL DESC LIMIT 1 FOR UPDATE)""".trimMargin()

}
