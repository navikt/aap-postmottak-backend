package no.nav.aap.postmottak.joarkavstemmer

import no.nav.aap.lookup.repository.Repository

interface AvstemmingsrapportRepository : Repository {
    fun lagre(rapport: Avstemmingsrapport)
    fun hentSiste(): Avstemmingsrapport?
}
