package no.nav.aap.postmottak.flyt.steg

object Avbrutt: StegResultat {
    override fun transisjon(): Transisjon {
        return AvbrytEtterAvklaring()
    }
}