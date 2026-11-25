package no.nav.aap.postmottak.under18

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class Under18Søknadsgrunnlag(
    val personId: Long,
    val søknader: List<JournalpostId>,
    val tarImotNyeSøknader: Boolean = true,
) {
    init {
        require(søknader.isNotEmpty()) { "Søknadsgrunnlaget må inneholde minst én søknad" }
        require(søknader.distinct() == søknader) { "Samme søknad finnes flere ganger i grunnlaget" }
    }

    fun leggTilSøknad(journalpostId: JournalpostId): Under18Søknadsgrunnlag {
        check(tarImotNyeSøknader) { "Under18-behandlingen tar ikke imot nye søknader" }
        return copy(søknader = søknader + journalpostId)
    }

    fun stengForNyeSøknader(): Under18Søknadsgrunnlag = copy(tarImotNyeSøknader = false)
}
