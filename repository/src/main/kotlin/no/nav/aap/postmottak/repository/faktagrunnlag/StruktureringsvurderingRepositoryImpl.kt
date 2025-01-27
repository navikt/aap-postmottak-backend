package no.nav.aap.postmottak.repository.faktagrunnlag


import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class StruktureringsvurderingRepositoryImpl(private val connection: DBConnection): StruktureringsvurderingRepository {
    
    companion object : Factory<StruktureringsvurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): StruktureringsvurderingRepositoryImpl {
            return StruktureringsvurderingRepositoryImpl(connection)
        }
    }
    
    override fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: Digitaliseringsvurdering) {
        val vurderingsId = connection.executeReturnKey(
            """
            INSERT INTO DIGITALISERINGSAVKLARING (KATEGORI, STRUKTURERT_DOKUMENT) VALUES (?, ?)
        """.trimIndent()
        ) { setParams {
            setEnumName(1, strukturertDokument.kategori)
            setString(2, strukturertDokument.strukturertDokument)
        } }

        connection.execute("""UPDATE DIGITALISERINGSVURDERING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?""") {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute("""
            INSERT INTO DIGITALISERINGSVURDERING_GRUNNLAG (BEHANDLING_ID, DIGITALISERINGSAVKLARING_ID) VALUES (?, ?)
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id); setLong(2, vurderingsId) }
        }
    }


    override fun hentStruktureringsavklaring(behandlingId: BehandlingId): Digitaliseringsvurdering? {
        return connection.queryFirstOrNull("""
            SELECT * FROM DIGITALISERINGSVURDERING_GRUNNLAG 
            JOIN DIGITALISERINGSAVKLARING ON DIGITALISERINGSAVKLARING.id = DIGITALISERINGSAVKLARING_ID
            WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Digitaliseringsvurdering(
                    row.getEnum("Kategori"),
                    row.getStringOrNull("strukturert_dokument")
                )
            }
        }
    }

    override fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        connection.execute("""
            INSERT INTO DIGITALISERINGSVURDERING_GRUNNLAG (DIGITALISERINGSAVKLARING_ID, BEHANDLING_ID)
            SELECT DIGITALISERINGSAVKLARING_ID, ? FROM DIGITALISERINGSVURDERING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams {
                setLong(1, tilBehandlingId.id)
                setLong(2, fraBehandlingId.id)
            }
        }
    }

}
