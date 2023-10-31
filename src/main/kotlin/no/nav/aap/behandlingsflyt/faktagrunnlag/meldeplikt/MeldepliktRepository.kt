package no.nav.aap.behandlingsflyt.faktagrunnlag.meldeplikt

import no.nav.aap.behandlingsflyt.avklaringsbehov.meldeplikt.Fritaksvurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object MeldepliktRepository {

    private var grunnlagene = HashMap<BehandlingId, MeldepliktGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: BehandlingId, vurderinger: List<Fritaksvurdering>) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                MeldepliktGrunnlag(
                    behandlingId = behandlingId,
                    vurderinger = vurderinger,
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

    fun hentHvisEksisterer(behandlingId: BehandlingId): MeldepliktGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: BehandlingId): MeldepliktGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
