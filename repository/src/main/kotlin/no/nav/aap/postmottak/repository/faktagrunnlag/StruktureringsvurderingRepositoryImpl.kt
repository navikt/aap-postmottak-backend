package no.nav.aap.postmottak.repository.faktagrunnlag


import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.Struktureringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class StruktureringsvurderingRepositoryImpl(private val connection: DBConnection): StruktureringsvurderingRepository {
    
    companion object : Factory<StruktureringsvurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): StruktureringsvurderingRepositoryImpl {
            return StruktureringsvurderingRepositoryImpl(connection)
        }
    }
    
    override fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String) {
        val vurderingsId = connection.executeReturnKey(
            """
            INSERT INTO DIGITALISERINGSAVKLARING (STRUKTURERT_DOKUMENT) VALUES (
            ?)
        """.trimIndent()
        ) { setParams { setString(1, strukturertDokument) } }

        connection.execute("""UPDATE STRUKTURERINGAVKLARING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?""") {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute("""
            INSERT INTO STRUKTURERINGAVKLARING_GRUNNLAG (BEHANDLING_ID, DIGITALISERINGSAVKLARING_ID) VALUES (?, ?)
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id); setLong(2, vurderingsId) }
        }
    }


    override fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering? {
        return connection.queryFirstOrNull("""
            SELECT * FROM STRUKTURERINGAVKLARING_GRUNNLAG 
            JOIN DIGITALISERINGSAVKLARING ON DIGITALISERINGSAVKLARING.id = DIGITALISERINGSAVKLARING_ID
            WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Struktureringsvurdering(
                    row.getString("strukturert_dokument")
                )
            }
        }
    }

    override fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        connection.execute("""
            INSERT INTO STRUKTURERINGAVKLARING_GRUNNLAG (DIGITALISERINGSAVKLARING_ID, BEHANDLING_ID)
            SELECT DIGITALISERINGSAVKLARING_ID, ? FROM STRUKTURERINGAVKLARING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams {
                setLong(1, tilBehandlingId.id)
                setLong(2, fraBehandlingId.id)
            }
        }
    }

}
