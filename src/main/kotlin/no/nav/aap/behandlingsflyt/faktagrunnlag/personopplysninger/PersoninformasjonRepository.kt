package no.nav.aap.behandlingsflyt.faktagrunnlag.personopplysninger

import no.nav.aap.behandlingsflyt.behandling.Behandling
import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import java.util.concurrent.atomic.AtomicLong

object PersoninformasjonRepository {

    private var grunnlagene = HashMap<BehandlingId, PersoninfoGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun hentHvisEksisterer(behandlingId: BehandlingId): PersoninfoGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun lagre(behandlingId: BehandlingId, personinfo: Personinfo?) {
        synchronized(LOCK) {
            if (personinfo != null) {
                grunnlagene.put(
                    behandlingId,
                    PersoninfoGrunnlag(key.addAndGet(1L), personinfo)
                )
            } else {
                grunnlagene.remove(behandlingId)
            }
        }
    }

    fun kopier(fraBehandling: Behandling, tilBehandling: Behandling) {
        synchronized(LOCK) {
            val eksisterendeGrunnlag = hentHvisEksisterer(fraBehandling.id)

            if (eksisterendeGrunnlag != null) {
                grunnlagene[tilBehandling.id] = eksisterendeGrunnlag
            }
        }
    }
}
