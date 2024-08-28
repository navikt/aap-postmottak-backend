package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.dbconnect.Row
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository, BehandlingFlytRepository {

    override fun opprettBehandling(typeBehandling: TypeBehandling): Behandling {

        val query = """
            INSERT INTO BEHANDLING (referanse, status, type)
                 VALUES (?, ?, ?)
            """.trimIndent()
        val behandlingsreferanse = BehandlingReferanse()
        val behandlingId = connection.executeReturnKey(query) {
            setParams {
                setUUID(1, behandlingsreferanse.referanse)
                setEnumName(2, Status.OPPRETTET)
                setString(3, typeBehandling.identifikator())
            }
        }


        val behandling = Behandling(
            id = BehandlingId(behandlingId),
            referanse = behandlingsreferanse,
            sakId = SakId(1),
            typeBehandling = typeBehandling,
            årsaker = emptyList(),
            versjon = 0
        )

        return behandling
    }

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            referanse = BehandlingReferanse(row.getUUID("referanse")),
            sakId = SakId(1),//row.getLongOrNull("sak_id")?.let { SakId(it) },
            typeBehandling = TypeBehandling.from(row.getString("type")),
            status = row.getEnum("status"),
            stegHistorikk = hentStegHistorikk(behandlingId),
            versjon = row.getLong("versjon")
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
            SELECT * FROM BEHANDLING WHERE id = ?
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
            SELECT * FROM BEHANDLING WHERE referanse = ?
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

}
