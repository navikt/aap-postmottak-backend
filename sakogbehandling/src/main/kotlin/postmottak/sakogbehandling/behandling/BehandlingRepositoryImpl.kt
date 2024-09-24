package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Vurderinger
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.postmottak.kontrakt.journalpost.Status
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.sak.Status
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.Status
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling
import java.time.LocalDateTime

class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository, BehandlingFlytRepository {

    private val vurderingRepository = AvklaringRepositoryImpl(connection)

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

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            journalpostId = JournalpostId(row.getLong("journalpost_id")),
            status = row.getEnum("status"),
            stegHistorikk = hentStegHistorikk(behandlingId),
            versjon = row.getLong("versjon"),
            vurderinger = hentVurderingerForBehandling(behandlingId)
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

    private fun hentVurderingerForBehandling(behandlingId: BehandlingId) = Vurderinger(
        saksvurdering = vurderingRepository.hentSakAvklaring(behandlingId),
        avklarTemaVurdering = vurderingRepository.hentTemaAvklaring(behandlingId),
        kategorivurdering = vurderingRepository.hentKategoriAvklaring(behandlingId),
        struktureringsvurdering = vurderingRepository.hentStruktureringsavklaring(behandlingId)
    )

}
