package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.TemaVurdeirng
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class AvklarTemaRepositoryImpl(private val connection: DBConnection): AvklarTemaRepository {

    companion object : Factory<AvklarTemaRepositoryImpl> {
        override fun konstruer(connection: DBConnection): AvklarTemaRepositoryImpl {
            return AvklarTemaRepositoryImpl(connection)
        }
    }
    
    override fun lagreTemaAvklaring(behandlingId: BehandlingId, vurdering: Boolean) {
        val vurderingId = connection.executeReturnKey(
            """
            INSERT INTO TEMAVURDERING (SKAL_TIL_AAP) VALUES (?)
        """.trimIndent()
        ) {
            setParams {
                setBoolean(1, vurdering)
            }
        }

        connection.execute("""UPDATE TEMAVURDERING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?""") {setParams { setLong(1, behandlingId.id) }}

        connection.execute("""INSERT INTO TEMAVURDERING_GRUNNLAG (BEHANDLING_ID, TEMAVURDERING_ID) VALUES (?, ?)""") {
            setParams { setLong(1, behandlingId.id); setLong(2, vurderingId) }
        }

    }

    override fun hentTemaAvklaring(behandlingId: BehandlingId): TemaVurdeirng? {
        return connection.queryFirstOrNull("""
            SELECT * FROM TEMAVURDERING_GRUNNLAG
            JOIN TEMAVURDERING ON TEMAVURDERING.ID = TEMAVURDERING_ID
            WHERE BEHANDLING_ID = ? AND AKTIV 
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row -> TemaVurdeirng (
                row.getBoolean(
                    "skal_til_aap"
                )
            )
            }
        }
    }

    override fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        connection.execute("""
            INSERT INTO TEMAVURDERING_GRUNNLAG (TEMAVURDERING_ID, BEHANDLING_ID) 
            SELECT TEMAVURDERING_ID, ? FROM TEMAVURDERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV 
        """.trimIndent()) {
            setParams {
                setLong(2, fraBehandlingId.id)
                setLong(1, tilBehandlingId.id)
            }
        }
    }


}