package no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt

import java.math.BigDecimal
import java.math.RoundingMode

class Beløp(private val verdi: BigDecimal) {

    init {
        verdi.setScale(2, RoundingMode.HALF_UP)
    }

    fun verdi(): BigDecimal {
        return verdi
    }
}