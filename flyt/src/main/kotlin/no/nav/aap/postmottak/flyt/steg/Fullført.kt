package no.nav.aap.postmottak.flyt.steg

object Fullført : StegResultat {
    override fun transisjon(): Transisjon {
        return Fortsett
    }
}