package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandsVurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object BistandsRepository {

    private var grunnlagene = HashMap<BehandlingId, BistandsGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: BehandlingId, bistandsVurdering: BistandsVurdering) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                BistandsGrunnlag(
                    behandlingId = behandlingId,
                    vurdering = bistandsVurdering,
                    id = key.addAndGet(1)
                )
            )
        }
    }

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            grunnlagene[fraBehandling.id]?.let { eksisterendeGrunnlag ->
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): BistandsGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: BehandlingId): BistandsGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
