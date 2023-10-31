package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object YrkesskadeRepository {

    private var grunnlagene = HashMap<BehandlingId, YrkesskadeGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun lagre(behandlingId: BehandlingId, yrkesskader: Yrkesskader?) {
        synchronized(LOCK) {
            if (yrkesskader != null) {
                grunnlagene.put(
                    behandlingId,
                    YrkesskadeGrunnlag(behandlingId = behandlingId, yrkesskader = yrkesskader, id = key.addAndGet(1))
                )
            } else {
                grunnlagene.remove(behandlingId)
            }
        }
    }

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            grunnlagene[fraBehandling.id]?.let { eksisterendeGrunnlag ->
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }

    fun hentHvisEksisterer(behandlingId: BehandlingId): YrkesskadeGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }
}
