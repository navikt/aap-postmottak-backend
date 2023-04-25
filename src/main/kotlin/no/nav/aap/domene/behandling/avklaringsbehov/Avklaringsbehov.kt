package no.nav.aap.domene.behandling.avklaringsbehov

import no.nav.aap.flyt.StegStatus
import no.nav.aap.flyt.StegType

class Avklaringsbehov(var definisjon: Definisjon, var status: Status, val historikk: MutableList<Endring> = mutableListOf()) {

    // TODO: Trenger identifikator, knyttning til steg, type,

    fun reåpne() {
        historikk += Endring(status = Status.OPPRETTET, begrunnelse = "", endretAv = "system")
    }

    fun erÅpent(): Boolean {
        return true
    }

    fun skalStoppeHer(stegType: StegType, stegStatus: StegStatus): Boolean {
        return definisjon.løsesISteg(stegType) && definisjon.påStegStatus(stegStatus) && status == Status.OPPRETTET
    }

    fun løs(begrunnelse: String, endretAv: String) {
        historikk.add(Endring(status = Status.AVSLUTTET, begrunnelse = begrunnelse, endretAv = endretAv))
        status = Status.AVSLUTTET
    }
}
