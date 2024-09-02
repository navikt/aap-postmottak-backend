package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning

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
