package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklaringsbehovLøsning

class LøsAvklaringsbehovBehandlingHendelse(
    private val løsning: AvklaringsbehovLøsning,
    val ingenEndringIGruppe: Boolean = false,
    val behandlingVersjon: Long,
    val bruker: Bruker
) {

    fun behov(): AvklaringsbehovLøsning {
        return løsning
    }
}
