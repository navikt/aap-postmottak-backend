package no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

class SaksnummerRepository {

    fun hentSaksnummre(behandlingId: BehandlingId): List<Saksnummer> {
        return emptyList()
    }

    fun lagreSaksnummer(behandlingId: BehandlingId, saksnummre: List<Saksnummer>) {

    }

}
