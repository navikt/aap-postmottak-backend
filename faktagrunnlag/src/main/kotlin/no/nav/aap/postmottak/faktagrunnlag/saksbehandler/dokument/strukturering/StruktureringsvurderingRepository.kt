package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering


import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class StruktureringsvurderingRepository(private val connection: DBConnection) {


    fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String) {
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


    fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering? {
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

}
