package no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom.Yrkesskadevurdering
import no.nav.aap.behandlingsflyt.behandling.BehandlingId

class SykdomsGrunnlag(
    val id: Long,
    val behandlingId: BehandlingId,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
) {
    fun erKonsistent(): Boolean {
        if (sykdomsvurdering == null) {
            return false
        }
        if (yrkesskadevurdering?.erÅrsakssammenheng == true) {
            return sykdomsvurdering.nedreGrense == NedreGrense.TRETTI
        }
        return sykdomsvurdering.nedreGrense == NedreGrense.FEMTI
    }
}
