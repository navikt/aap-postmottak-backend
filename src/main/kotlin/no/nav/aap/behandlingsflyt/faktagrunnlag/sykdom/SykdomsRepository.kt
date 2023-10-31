package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object SykdomsRepository {

    private var grunnlagene = HashMap<BehandlingId, SykdomsGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: BehandlingId, yrkesskadevurdering: Yrkesskadevurdering?, sykdomsvurdering: Sykdomsvurdering?) {
        synchronized(LOCK) {
            grunnlagene.put(
                behandlingId,
                SykdomsGrunnlag(
                    behandlingId = behandlingId,
                    yrkesskadevurdering = yrkesskadevurdering,
                    sykdomsvurdering = sykdomsvurdering,
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

    fun hentHvisEksisterer(behandlingId: BehandlingId): SykdomsGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun hent(behandlingId: BehandlingId): SykdomsGrunnlag {
        synchronized(LOCK) {
            return grunnlagene.getValue(behandlingId)
        }
    }
}
