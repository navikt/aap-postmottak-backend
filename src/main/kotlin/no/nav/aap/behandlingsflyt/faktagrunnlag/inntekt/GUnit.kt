package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Faktor av antall G for representasjon av størrelsen på det maksnimalet grunnlaget
 */
class GUnit(verdi: BigDecimal) {
    private val verdi = verdi.setScale(10, RoundingMode.HALF_UP)

    fun verdi(): BigDecimal {
        return verdi
    }

    override fun toString(): String {
        return "GUnit(verdi=$verdi)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GUnit

        return verdi == other.verdi
    }

    override fun hashCode(): Int {
        return verdi.hashCode()
    }
}
