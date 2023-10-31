package no.nav.aap.behandlingsflyt.faktagrunnlag.bistand

import no.nav.aap.behandlingsflyt.avklaringsbehov.bistand.BistandsVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

class BistandsGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: BistandsVurdering?,
)