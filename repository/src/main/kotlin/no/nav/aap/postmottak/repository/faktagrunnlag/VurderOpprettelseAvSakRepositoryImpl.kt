package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.VurderOpprettelseAvSakVurdering
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg

class VurderOpprettelseAvSakRepositoryImpl(private val connection: DBConnection) :
    VurderOpprettelseAvSakRepository {
    companion object : Factory<VurderOpprettelseAvSakRepositoryImpl> {
        override fun konstruer(connection: DBConnection): VurderOpprettelseAvSakRepositoryImpl {
            return VurderOpprettelseAvSakRepositoryImpl(connection)
        }
    }

    override fun lagre(behandlingId: BehandlingId, vurdering: VurderOpprettelseAvSakVurdering) {
        connection.execute(
            """UPDATE VURDER_OPPRETTELSE_AV_SAK_VURDERING SET AKTIV = FALSE WHERE BEHANDLING_ID = ?"""
        ) {
            setParams { setLong(1, behandlingId.id) }
        }

        connection.execute(
            """
            INSERT INTO VURDER_OPPRETTELSE_AV_SAK_VURDERING (BEHANDLING_ID, VALG, BEGRUNNELSE)
            VALUES (?, ?, ?)
            """.trimIndent()
        ) {
            setParams {
                setLong(1, behandlingId.id)
                setString(2, vurdering.valg?.name)
                setString(3, vurdering.begrunnelse)
            }
        }
    }

    override fun hentHvisEksisterer(behandlingId: BehandlingId): VurderOpprettelseAvSakVurdering? {
        return connection.queryFirstOrNull(
            """
            SELECT VALG, BEGRUNNELSE FROM VURDER_OPPRETTELSE_AV_SAK_VURDERING
            WHERE BEHANDLING_ID = ? AND AKTIV
            """.trimIndent()
        ) {
            setParams { setLong(1, behandlingId.id) }
            setRowMapper { row ->
                VurderOpprettelseAvSakVurdering(
                    valg = row.getStringOrNull("valg")?.let(VurderOpprettelseAvSakValg::valueOf),
                    begrunnelse = row.getStringOrNull("begrunnelse"),
                )
            }
        }
    }
}

