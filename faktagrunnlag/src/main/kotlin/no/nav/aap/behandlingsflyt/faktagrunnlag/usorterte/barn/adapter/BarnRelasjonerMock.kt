package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.barn.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.barn.Barn
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident

object BarnRelasjonerMock {
    fun innhent(identer: List<Ident>, rettighetsperiode: Periode): List<Barn> {
        return listOf()
    }
}