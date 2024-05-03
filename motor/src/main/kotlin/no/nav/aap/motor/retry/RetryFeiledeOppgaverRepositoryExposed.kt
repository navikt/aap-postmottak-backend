package no.nav.aap.motor.retry

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection

class RetryFeiledeOppgaverRepositoryExposed(connection: DBConnection) {
    private val retryFeiledeOppgaverRepository = RetryFeiledeOppgaverRepository(connection)

    fun markerAlleFeiledeForKlare(): Int {
        return retryFeiledeOppgaverRepository.markerAlleFeiledeForKlare()
    }
}
