package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.bistand

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class BistandGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: BistandVurdering,
)
