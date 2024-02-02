package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt

import no.nav.aap.verdityper.Beløp
import no.nav.aap.verdityper.GUnit
import java.time.Year

class InntektPerÅr(val år: Year, val beløp: Beløp) :
    Comparable<no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr> {
    constructor(år: Int, beløp: Beløp) : this(Year.of(år), beløp)

    fun gUnit(): GUnit {
        return no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.Grunnbeløp.finnGUnit(år, beløp)
    }

    override fun compareTo(other: no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr): Int {
        return this.år.compareTo(other.år)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.inntekt.InntektPerÅr

        if (år != other.år) return false
        if (beløp != other.beløp) return false

        return true
    }

    override fun hashCode(): Int {
        var result = år.hashCode()
        result = 31 * result + beløp.hashCode()
        return result
    }
}
