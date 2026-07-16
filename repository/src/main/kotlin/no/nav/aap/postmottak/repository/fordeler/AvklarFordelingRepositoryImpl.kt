package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.fordeler.arena.AvklarFordelingVurdering
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class AvklarFordelingRepositoryImpl(private val connection: DBConnection): AvklarFordelingRepository {
    companion object : Factory<AvklarFordelingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): AvklarFordelingRepositoryImpl {
            return AvklarFordelingRepositoryImpl(connection)
        }
    }

    override fun hentVurderingHvisEksisterer(behandlingId: BehandlingId): AvklarFordelingVurdering? {
        return connection.queryFirstOrNull(
            """
            SELECT v.VURDERT_AV, v.FAGSYSTEM, v.OPPRETTET_TID, v.KOMMENTAR
            FROM AVKLAR_FORDELING_GRUNNLAG g
            JOIN AVKLAR_FORDELING_VURDERING v ON v.ID = g.AVKLAR_FORDELING_VURDERING_ID
            WHERE g.BEHANDLING_ID = ? AND g.AKTIV = TRUE
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                AvklarFordelingVurdering(
                    system = AapSystem.fraString(row.getString("FAGSYSTEM")),
                    vurdertAv = row.getString("VURDERT_AV"),
                    vurdertTidspunkt = row.getLocalDateTime("OPPRETTET_TID"),
                    kommentar = row.getStringOrNull("KOMMENTAR"),
                )
            }
        }
    }

    override fun lagreVurdering(behandlingId: BehandlingId, vurdering: AvklarFordelingVurdering) {
        val vurderingId = connection.executeReturnKey(
            """
            INSERT INTO AVKLAR_FORDELING_VURDERING (VURDERT_AV, FAGSYSTEM, OPPRETTET_TID, KOMMENTAR)
            VALUES (?, ?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setString(1, vurdering.vurdertAv)
                setString(2, vurdering.system.name)
                setLocalDateTime(3, vurdering.vurdertTidspunkt)
                setString(4, vurdering.kommentar)
            }
        }

        connection.execute(
            """UPDATE AVKLAR_FORDELING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?"""
        ) {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute(
            """
            INSERT INTO AVKLAR_FORDELING_GRUNNLAG (BEHANDLING_ID, AVKLAR_FORDELING_VURDERING_ID)
            VALUES (?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setLong(2, vurderingId)
            }
        }
    }
}
