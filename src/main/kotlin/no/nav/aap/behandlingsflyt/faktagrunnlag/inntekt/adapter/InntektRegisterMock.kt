package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter

import no.nav.aap.behandlingsflyt.sak.Ident
import java.time.Year

object InntektRegisterMock {
    private val skader = HashMap<Ident, List<InntektPerÅr>>()

    fun innhent(identer: List<Ident>, år: List<Year>): List<InntektPerÅr> {
        return skader.filter { entry -> identer.contains(entry.key) }
            .flatMap { entry -> entry.value.filter { år.contains(it.år) } }
    }

    fun konstruer(ident: Ident, inntekterPerÅr: List<InntektPerÅr>) {
        skader[ident] = inntekterPerÅr
    }
}