package no.nav.aap.postmottak.sakogbehandling.sak

import no.nav.aap.verdityper.sakogbehandling.SakId

interface SakFlytRepository {

    fun oppdaterSakStatus(sakId: SakId, status: Status)

}

