package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.db.SakRepositoryImpl
import no.nav.aap.verdityper.sakogbehandling.SakId

interface SakFlytRepository {

    fun oppdaterSakStatus(sakId: SakId, status: Status)

}

fun SakFlytRepository(connection: DBConnection): SakFlytRepository {
    return SakRepositoryImpl(connection)
}