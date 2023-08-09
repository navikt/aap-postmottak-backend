package no.nav.aap.domene.behandling

import no.nav.aap.domene.behandling.grunnlag.GrunnlagKopierer
import java.util.Optional
import java.util.concurrent.atomic.AtomicLong

object BehandlingTjeneste {
    private var behandliger = HashMap<Long, Behandling>()
    private val key = AtomicLong()

    private val LOCK = Object()

    fun hent(behandlingId: Long): Behandling {
        synchronized(LOCK) {
            return behandliger.getValue(behandlingId)
        }
    }

    fun finnSisteBehandlingFor(sakId: Long): Behandling? {
        synchronized(LOCK) {
            return behandliger.values
                .filter { behandling -> behandling.sakId == sakId }
                .maxOrNull()
        }
    }

    fun opprettBehandling(sakId: Long): Behandling {
        synchronized(LOCK) {
            val sisteBehandlingFor = finnSisteBehandlingFor(sakId)
            val erSisteBehandlingAvsluttet = sisteBehandlingFor?.status()?.erAvsluttet() ?: true

            if (erSisteBehandlingAvsluttet) {
                val key1 = key.addAndGet(1)
                val behandlingType = utledBehandlingType(sisteBehandlingFor != null)
                val behandling = Behandling(key1, sakId, behandlingType)
                behandliger[key1] = behandling

                if (sisteBehandlingFor != null) {
                    GrunnlagKopierer.overfør(sisteBehandlingFor, behandling)
                }

                return behandliger.getValue(key1)
            } else {
                throw IllegalStateException("Siste behandling er ikke avsluttet")
            }
        }
    }

    private fun utledBehandlingType(present: Boolean): BehandlingType {
        if (present) {
            return Revurdering
        }
        return Førstegangsbehandling
    }
}
