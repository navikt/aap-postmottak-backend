package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.verdityper.sakogbehandling.SakId
import no.nav.aap.verdityper.sakogbehandling.TypeBehandling

data class FlytKontekst(
    val sakId: SakId,
    val behandlingId: BehandlingId,
    val behandlingType: TypeBehandling
)