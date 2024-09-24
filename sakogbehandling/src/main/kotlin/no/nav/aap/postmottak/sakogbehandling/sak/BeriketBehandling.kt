package no.nav.aap.postmottak.sakogbehandling.sak

import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class BeriketBehandling(
    val behandling: Behandling,
    val tilstand: BehandlingTilstand,
    val sisteAvsluttedeBehandling: BehandlingId?
) {
    fun skalKopierFraSisteBehandling(): Boolean {
        return false
    }
}

enum class BehandlingTilstand {
    EKSISTERENDE, NY
}
