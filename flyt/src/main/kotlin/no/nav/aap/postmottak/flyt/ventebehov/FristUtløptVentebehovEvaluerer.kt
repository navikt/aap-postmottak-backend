package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov

internal object FristUtløptVentebehovEvaluerer {

    fun ansesSomLøst(avklaringsbehov: Avklaringsbehov): Boolean {
        return avklaringsbehov.erÅpent() && avklaringsbehov.fristUtløpt()
    }
}
