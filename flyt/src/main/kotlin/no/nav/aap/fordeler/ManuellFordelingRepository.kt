package no.nav.aap.fordeler

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.Fagsystem
import no.nav.aap.postmottak.journalpostogbehandling.Ident

interface ManuellFordelingRepository : Repository {
    fun fordelTilFagsystem(ident: Ident): Fagsystem?
}