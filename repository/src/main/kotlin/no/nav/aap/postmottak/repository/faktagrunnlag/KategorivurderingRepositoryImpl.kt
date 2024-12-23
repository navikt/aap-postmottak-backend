package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class KategorivurderingRepositoryImpl(private val connection: DBConnection): KategoriVurderingRepository {

    companion object : Factory<KategorivurderingRepositoryImpl> {
        override fun konstruer(connection: DBConnection): KategorivurderingRepositoryImpl {
            return KategorivurderingRepositoryImpl(connection)
        }
    }

    override fun lagreKategoriseringVurdering(behandlingId: BehandlingId, kategori: InnsendingType) {
        val vurdeirngId = connection.executeReturnKey(
            """
            INSERT INTO KATEGORIAVKLARING (KATEGORI) VALUES (?)
        """.trimIndent()
        ) {
            setParams {
                setEnumName(1, kategori)
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

    override fun hentKategoriAvklaring(behandlingId: BehandlingId): KategoriVurdering? {
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

    override fun kopier(fraBehandlingId: BehandlingId, tilBehandlingId: BehandlingId) {
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