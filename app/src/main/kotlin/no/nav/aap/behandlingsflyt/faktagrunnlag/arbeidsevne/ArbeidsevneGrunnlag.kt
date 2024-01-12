package no.nav.aap.behandlingsflyt.faktagrunnlag.arbeidsevne

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.arbeidsevne.Arbeidsevne
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class ArbeidsevneGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val vurdering: Arbeidsevne,
)
