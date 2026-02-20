package no.nav.aap.postmottak.exception

interface FlytOperasjonException {

    fun body(): ErrorRespons
}