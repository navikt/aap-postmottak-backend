package no.nav.aap.fordeler

import no.nav.aap.lookup.repository.Repository

interface RegelRepository: Repository {
    fun hentRegelresultat(journalpostId: Long): Regelresultat
    fun lagre(journalpostId: Long, regelresultat: Regelresultat)
}