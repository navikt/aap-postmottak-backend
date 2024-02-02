package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.meldeplikt

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

data class MeldepliktGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurderinger: List<Fritaksvurdering>
)
