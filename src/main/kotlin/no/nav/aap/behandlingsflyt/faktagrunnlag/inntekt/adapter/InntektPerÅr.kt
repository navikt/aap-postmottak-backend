package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import java.time.Year

class InntektPerÅr(val år: Year, val beløp: Beløp) : Comparable<InntektPerÅr> {
    override fun compareTo(other: InntektPerÅr): Int {
        return this.år.compareTo(other.år)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as InntektPerÅr

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