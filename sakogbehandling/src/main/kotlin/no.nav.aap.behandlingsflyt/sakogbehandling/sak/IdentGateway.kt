package no.nav.aap.behandlingsflyt.sakogbehandling.sak

import no.nav.aap.verdityper.sakogbehandling.Ident

interface IdentGateway {
    // TODO: returner execption, option, result eller emptylist
    suspend fun hentAlleIdenterForPerson(ident: Ident): List<Ident>
}