package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.avklaringsbehov.arbeidsevne.Arbeidsevne
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

class ArbeidsevneGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: Arbeidsevne,
)
