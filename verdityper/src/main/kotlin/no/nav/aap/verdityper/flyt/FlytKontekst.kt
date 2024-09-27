package no.nav.aap.verdityper.flyt

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling

data class FlytKontekst(
    val behandlingId: BehandlingId,
    val behandlingType: TypeBehandling
)