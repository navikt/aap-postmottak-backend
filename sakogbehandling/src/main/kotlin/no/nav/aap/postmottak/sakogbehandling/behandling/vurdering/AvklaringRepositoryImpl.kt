package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering


import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class AvklaringRepositoryImpl(private val connection: DBConnection) : AvklaringRepository {


    override fun lagreStrukturertDokument(behandlingId: BehandlingId, strukturertDokument: String) {
        connection.execute(
            """
            INSERT INTO DIGITALISERINGSAVKLARING (BEHANDLING_ID, STRUKTURERT_DOKUMENT) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setString(2, strukturertDokument)
            }
        }
    }




    override fun hentStruktureringsavklaring(behandlingId: BehandlingId): Struktureringsvurdering? {
        return connection.queryFirstOrNull(vurderingQuery("DIGITALISERINGSAVKLARING")) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                Struktureringsvurdering(
                    row.getString("strukturert_dokument")
                )
            }
        }
    }

    private fun vurderingQuery(tableName: String) =
        """SELECT * FROM $tableName 
            WHERE BEHANDLING_ID = ?
            ORDER BY TIDSSTEMPEL DESC LIMIT 1
        """.trimMargin()

}