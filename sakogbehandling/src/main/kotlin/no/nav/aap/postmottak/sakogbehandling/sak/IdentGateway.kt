package no.nav.aap.postmottak.sakogbehandling.sak

import no.nav.aap.verdityper.sakogbehandling.Ident

interface IdentGateway {
    fun hentAlleIdenterForPerson(ident: Ident): List<Ident>
}