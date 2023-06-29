package no.nav.aap.domene.behandling

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

    fun finnSisteBehandlingFor(sakId: Long): Optional<Behandling> {
        synchronized(LOCK) {
            return Optional.ofNullable(behandliger.values
                .filter { behandling -> behandling.sakId == sakId }
                .maxOrNull())
        }
    }

    fun opprettBehandling(sakId: Long): Behandling {
        synchronized(LOCK) {
            val sisteBehandlingFor = finnSisteBehandlingFor(sakId)
            val erSisteBehandlingAvsluttet = sisteBehandlingFor.map { it.status().erAvsluttet() }.orElse(true)

            if (erSisteBehandlingAvsluttet) {
                val key1 = key.addAndGet(1)
                val behandlingType = utledBehandlingType(sisteBehandlingFor.isPresent)
                behandliger[key1] = Behandling(key1, sakId, behandlingType)
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
