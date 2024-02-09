package no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger

import no.nav.aap.verdityper.sakogbehandling.Ident

interface PersonopplysningGateway {
    fun innhent(identer: List<Ident>): Set<Personopplysning>
}