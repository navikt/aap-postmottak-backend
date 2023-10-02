package no.nav.aap.behandlingsflyt.domene.behandling.grunnlag.yrkesskade

import no.nav.aap.behandlingsflyt.domene.person.Ident
import no.nav.aap.behandlingsflyt.domene.Periode

object YrkesskadeRegisterMock {

    private val skader = HashMap<Ident, no.nav.aap.behandlingsflyt.domene.Periode>()

    fun innhent(identer: List<Ident>, periode: no.nav.aap.behandlingsflyt.domene.Periode): List<no.nav.aap.behandlingsflyt.domene.Periode> {
        return skader.filter { entry -> identer.contains(entry.key) }
            .filter { entry -> entry.value.overlapper(periode) }
            .map { it.value }
    }

    fun konstruer(ident: Ident, periode: no.nav.aap.behandlingsflyt.domene.Periode) {
        skader[ident] = periode
    }
}
