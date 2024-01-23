package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.verdityper.sakogbehandling.SakId

class SakService(connection: DBConnection) {

    private val sakRepository = sakRepository(connection)

    fun hent(sakId: SakId): Sak {
        return sakRepository.hent(sakId)
    }
    fun hent(saksnummer: Saksnummer): Sak {
        return sakRepository.hent(saksnummer)
    }
}
