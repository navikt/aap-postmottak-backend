package no.nav.aap.behandlingsflyt.domene.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.flyt.StegStatus

enum class Vurderingspunkt(val stegStatus: StegStatus) {
    INN(StegStatus.INNGANG),
    UT(StegStatus.UTGANG)
}
