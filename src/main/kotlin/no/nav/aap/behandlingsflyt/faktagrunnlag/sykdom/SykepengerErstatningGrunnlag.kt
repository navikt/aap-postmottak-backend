package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.SykepengerVurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

class SykepengerErstatningGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: SykepengerVurdering?
)
