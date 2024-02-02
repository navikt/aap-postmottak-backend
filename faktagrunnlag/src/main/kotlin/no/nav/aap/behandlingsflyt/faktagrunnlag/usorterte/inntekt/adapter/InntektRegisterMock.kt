package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.adapter

import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.Year

object InntektRegisterMock {
    private val inntekter =
        HashMap<Ident, List<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr>>()

    fun innhent(
        identer: List<Ident>,
        år: Set<Year>
    ): Set<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr> {
        val resultat: MutableSet<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr> =
            mutableSetOf()
        for (year in år) {
            val relevanteIdenter = inntekter.filter { entry -> identer.contains(entry.key) }
            val summerteInntekter = relevanteIdenter
                .flatMap { it.value }
                .filter { it.år == year }
                .sumOf { it.beløp.verdi() }

            resultat.add(
                no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr(
                    year,
                    Beløp(summerteInntekter)
                )
            )
        }
        return resultat.toSortedSet()
    }

    fun konstruer(
        ident: Ident,
        inntekterPerÅr: List<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr>
    ) {
        inntekter[ident] = inntekterPerÅr
    }
}