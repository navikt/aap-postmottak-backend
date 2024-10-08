package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.Params
import no.nav.aap.komponenter.dbconnect.Row
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.Vurderinger
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDateTime


class BehandlingRepositoryImpl(private val connection: DBConnection) : BehandlingRepository, BehandlingFlytRepository {

    private val vurderingRepository = AvklaringRepositoryImpl(connection)

    override fun opprettBehandling(journalpostId: JournalpostId): BehandlingId {

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

        return BehandlingId(behandlingId)
    }

    private fun mapBehandling(row: Row): Behandling {
        val behandlingId = BehandlingId(row.getLong("id"))
        return Behandling(
            id = behandlingId,
            journalpostId = JournalpostId(row.getLong("journalpost_id")),
            status = row.getEnum("status"),
            stegHistorikk = hentStegHistorikk(behandlingId),
            opprettetTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
            vurderinger = hentVurderingerForBehandling(behandlingId),
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

    override fun hentMedLås(behandlingId: BehandlingId, versjon: Long?): Behandling {
        val query = """
            WITH params (pId, pVersjon) as (values(?, ?))
            SELECT * FROM BEHANDLING b, params
            WHERE b.id = pId
            AND (b.versjon = pVersjon OR pVersjon is null)
            FOR UPDATE OF b
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, behandlingId.toLong()); setLong(2, versjon) }
    }

    override fun hentMedLås(journalpostId: JournalpostId, versjon: Long?): Behandling {
        val query = """
            WITH params (pId, pVersjon) as (values(?, ?))
            SELECT * FROM BEHANDLING b, params
            WHERE journalpost_id = pId
            AND (b.versjon = pVersjon OR pVersjon is null)
            FOR UPDATE OF b
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, journalpostId.referanse); setLong(2, versjon) }

    }

    override fun hent(journalpostId: JournalpostId, versjon: Long?): Behandling {
        val query = """
            WITH params (pId, pVersjon) as (values(?, ?))
            SELECT * FROM BEHANDLING b, params
            WHERE journalpost_id = pId
            AND (b.versjon = pVersjon OR pVersjon is null)
            """.trimIndent()

        return utførHentQuery(query) { setLong(1, journalpostId.referanse); setLong(2, versjon) }
    }

    private fun utførHentQuery(query: String, params: Params.() -> Unit): Behandling {
        return connection.queryFirst(query) {
            setParams(params)
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
