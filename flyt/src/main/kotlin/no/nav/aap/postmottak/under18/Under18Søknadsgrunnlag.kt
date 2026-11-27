package no.nav.aap.postmottak.under18

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class Under18Søknadsgrunnlag(
    val personId: Long,
    val søknader: Set<JournalpostId>,
    val tarImotNyeSøknader: Boolean = true,
) {
    init {
        require(søknader.isNotEmpty()) { "Søknadsgrunnlaget må inneholde minst én søknad" }
    }

    fun leggTilSøknad(journalpostId: JournalpostId): Under18Søknadsgrunnlag {
        check(tarImotNyeSøknader) { "Under18-behandlingen tar ikke imot nye søknader" }
        require(journalpostId !in søknader) { "Samme søknad finnes flere ganger i grunnlaget" }
        return copy(søknader = søknader + journalpostId)
    }

    fun stengForNyeSøknader(): Under18Søknadsgrunnlag = copy(tarImotNyeSøknader = false)
}
