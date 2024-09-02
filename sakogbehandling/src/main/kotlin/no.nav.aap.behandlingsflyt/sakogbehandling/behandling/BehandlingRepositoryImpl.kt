package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.verdityper.dokument.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository, BehandlingFlytRepository {

    override fun opprettBehandling(journalpostId: JournalpostId): Behandling {

        val query = """
            INSERT INTO BEHANDLING (referanse, status, type, journalpost_id)
                 VALUES (?, ?, ?, ?)
            """.trimIndent()
        val behandlingsreferanse = BehandlingReferanse()
        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setUUID(1, behandlingsreferanse.referanse)
                setEnumName(2, Status.OPPRETTET)
                setString(3, TypeBehandling.DokumentHåndtering.identifikator())
                setLong(4, journalpostId.identifikator)
            }
        }


        val behandling = Behandling(
            id = BehandlingId(behandlingId),
            journalpostId = journalpostId,
            referanse = behandlingsreferanse,
            sakId = SakId(1),
            versjon = 0,
            vurderinger = Vurderinger()
        )

        return behandling
    }

    override fun lagreGrovvurdeingVurdering(behandlingId: BehandlingId, vurdering: Boolean) {
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
            INSERT INTO DIGITALISERINGSAVKLARING (BEHANDLING_ID, KATEGORI) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, strukturertDokument)
            }
        }
    }

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            journalpostId = JournalpostId(row.getLong("journalpost_id")),
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            sakId = row.getLongOrNull("sak_id")?.let { SakId(it) },
            status = row.getEnum("status"),
            stegHistorikk = hentStegHistorikk(behandlingId),
            versjon = row.getLong("versjon"),
            vurderinger = Vurderinger(
                row.getBooleanOrNull("skal_til_aap")?.let(::Vurdering),
                row.getEnumOrNull<Brevkode, _>("kategori")?.let(::Vurdering),
                row.getStringOrNull("strukturert_dokument")?.let(::Vurdering),
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
            WHERE b.id = ?
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

    override fun hent(referanse: BehandlingReferanse): Behandling {
        val query = """
            SELECT * FROM BEHANDLING b
            ${vurderingJoinQuery("SKAL_TIL_AAP_AVKLARING")}
            ${vurderingJoinQuery("KATEGORIAVKLARING")}
            ${vurderingJoinQuery("DIGITALISERINGSAVKLARING")}
            WHERE referanse = ?
            """.trimIndent()

        return connection.queryFirst(query) {
            setParams {
                setUUID(1, referanse.referanse)
            }
            setRowMapper {
                mapBehandling(it)
            }
        }
    }

    private fun vurderingJoinQuery(tableName: String) =
        "LEFT JOIN (SELECT * FROM $tableName ORDER BY TIDSSTEMPEL DESC LIMIT 1) as $tableName ON $tableName.BEHANDLING_ID = b.ID "

}
