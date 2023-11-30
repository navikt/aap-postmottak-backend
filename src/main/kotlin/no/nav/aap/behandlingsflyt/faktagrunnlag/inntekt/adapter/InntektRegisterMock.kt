package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter

import no.nav.aap.behandlingsflyt.sak.Ident
import java.time.Year

object InntektRegisterMock {
    private val skader = HashMap<Ident, List<InntektPerÅr>>()

    fun innhent(identer: List<Ident>, år: Set<Year>): Set<InntektPerÅr> {
        return skader.filter { entry -> identer.contains(entry.key) }
            .flatMap { entry -> entry.value.filter { år.contains(it.år) } }
            .toSortedSet()
    }

    fun konstruer(ident: Ident, inntekterPerÅr: List<InntektPerÅr>) {
        skader[ident] = inntekterPerÅr
    }
}