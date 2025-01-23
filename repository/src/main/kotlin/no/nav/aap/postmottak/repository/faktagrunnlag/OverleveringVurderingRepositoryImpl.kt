package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class OverleveringVurderingRepositoryImpl(private val connection: DBConnection): OverleveringVurderingRepository {
    companion object : Factory<OverleveringVurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): OverleveringVurderingRepositoryImpl {
            return OverleveringVurderingRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, overleveringVurdering: OverleveringVurdering) {
        val vurderingId = connection.executeReturnKey(
            """
            INSERT INTO OVERLEVERING_VURDERING (SKAL_OVERLEVERES) VALUES (
            ?)
        """.trimIndent()
        ) { setParams { setBoolean(1, overleveringVurdering.skalOverleveresTilKelvin) } }
        
        connection.execute("""UPDATE OVERLEVERING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?""") {
            setParams { setLong(1, behandlingId.id) }
        }
        
        connection.execute("""
            INSERT INTO OVERLEVERING_GRUNNLAG (BEHANDLING_ID, OVERLEVERING_VURDERING_ID) VALUES (?, ?)
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id); setLong(2, vurderingId) }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): OverleveringVurdering? {
        return connection.queryFirstOrNull("""
            SELECT * FROM OVERLEVERING_GRUNNLAG 
            JOIN OVERLEVERING_VURDERING ON OVERLEVERING_VURDERING.id = OVERLEVERING_VURDERING_ID
            WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                OverleveringVurdering(
                    row.getBoolean("skal_overleveres")
                )
            }
        }
    }

    override fun kopier(fraBehandling: BehandlingId, tilBehandling: BehandlingId) {
        connection.execute("""
            INSERT INTO OVERLEVERING_GRUNNLAG (OVERLEVERING_VURDERING_ID, BEHANDLING_ID)
            SELECT OVERLEVERING_VURDERING_ID, ? FROM OVERLEVERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams {
                setLong(1, tilBehandling.id)
                setLong(2, fraBehandling.id)
            }
        }
    }
}