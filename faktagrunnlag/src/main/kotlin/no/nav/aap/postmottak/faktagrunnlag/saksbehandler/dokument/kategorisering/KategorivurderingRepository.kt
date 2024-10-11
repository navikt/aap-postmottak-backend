package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class KategorivurderingRepository(private val connection: DBConnection) {


    fun lagreKategoriseringVurdering(behandlingId: BehandlingId, brevkode: Brevkode) {
        connection.execute(
            """
            INSERT INTO KATEGORIAVKLARING (BEHANDLING_ID, KATEGORI) VALUES (
            ?, ?)
        """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.toLong())
                setEnumName(2, brevkode)
            }
        }
    }

    fun hentKategoriAvklaring(behandlingId: BehandlingId): no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurdering? {
        return connection.queryFirstOrNull("""SELECT * FROM KATEGORIAVKLARING 
            WHERE BEHANDLING_ID = ?
            ORDER BY TIDSSTEMPEL DESC LIMIT 1
        """) {
            setParams { setLong(1, behandlingId.toLong()) }
            setRowMapper { row ->
                no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurdering(
                    row.getEnum("kategori")
                )
            }
        }
    }
}