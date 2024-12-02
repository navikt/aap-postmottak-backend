package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class KategorivurderingRepository(private val connection: DBConnection) {


    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, brevkode: InnsendingType) {
        val vurdeirngId = connection.executeReturnKey(
            """
            INSERT INTO KATEGORIAVKLARING (KATEGORI) VALUES (?)
        """.trimIndent()
        ) {
            setParams {
                setEnumName(1, brevkode)
            }
        }

        connection.execute("""UPDATE KATEGORIAVKLARING_GRUNNLAG SET AKTIV = FALSE WHERE BEHANDLING_ID = ?""") {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute("""
            INSERT INTO KATEGORIAVKLARING_GRUNNLAG (BEHANDLING_ID, KATEGORIAVKLARING_ID) VALUES (?, ?)
        """.trimIndent()) {
            setParams { setLong(1, behandlingId.id); setLong(2, vurdeirngId) }
        }

    }

    fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering? {
        return connection.queryFirstOrNull("""SELECT KATEGORIAVKLARING.* FROM KATEGORIAVKLARING
            JOIN KATEGORIAVKLARING_GRUNNLAG ON KATEGORIAVKLARING_ID = KATEGORIAVKLARING.ID
            WHERE BEHANDLING_ID = ? AND AKTIV 
            ORDER BY TIDSSTEMPEL DESC LIMIT 1
        """) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                KategoriVurdering(
                    row.getEnum("kategori")
                )
            }
        }
    }

    fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
        connection.execute("""
            INSERT INTO KATEGORIAVKLARING_GRUNNLAG (KATEGORIAVKLARING_ID, BEHANDLING_ID)
            SELECT KATEGORIAVKLARING_ID, ? FROM KATEGORIAVKLARING_GRUNNLAG WHERE BEHANDLING_ID = ? AND AKTIV
        """.trimIndent()) {
            setParams {
                setLong(1, tilBehandlingId.id)
                setLong(2, fraBehandlingId.id)
            }
        }
    }
}