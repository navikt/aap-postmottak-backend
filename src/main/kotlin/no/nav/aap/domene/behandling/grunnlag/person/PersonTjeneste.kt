package no.nav.aap.domene.behandling.grunnlag.person

import java.util.Optional
import java.util.concurrent.atomic.AtomicLong

object PersonTjeneste {

    private var grunnlagene = HashMap<Long, PersoninfoGrunnlag>()

    private val key = AtomicLong()
    private val LOCK = Object()

    fun hentHvisEksisterer(behandlingId: Long): PersoninfoGrunnlag? {
        synchronized(LOCK) {
            return grunnlagene[behandlingId]
        }
    }

    fun lagre(behandlingId: Long, personinfoGrunnlag: Personinfo?) {
        synchronized(LOCK) {
            if (personinfoGrunnlag != null) {
                grunnlagene.put(
                    behandlingId,
                    PersoninfoGrunnlag() //FIXME
                )
            } else {
                grunnlagene.remove(behandlingId)
            }
        }
    }
}
