package no.nav.aap.behandlingsflyt.faktagrunnlag.register.Institusjonsopphold

import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Person

interface InstitusjonsoppholdGateway {
    fun innhent(person: Person): Institusjonsopphold?
}