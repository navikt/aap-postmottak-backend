package no.nav.aap.behandlingsflyt.sakogbehandling.sak.adapters

import no.nav.aap.verdityper.sakogbehandling.Ident

interface PdlGateway {
    // TODO: returner execption, option, result eller emptylist
    suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident>
}