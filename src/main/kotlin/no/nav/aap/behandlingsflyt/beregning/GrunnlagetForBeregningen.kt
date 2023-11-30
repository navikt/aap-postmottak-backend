package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.GUnit
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr
import java.math.BigDecimal

class GrunnlagetForBeregningen(
    private val inntekter: List<InntektPerÅr>
) {
    fun beregnGrunnlaget(): GUnit {
        if (inntekter.size == 1) {
            val inntektForSisteÅr = inntekter.first()
            return Grunnbeløp.finnGrunnlagsfaktor(inntektForSisteÅr.år, inntektForSisteÅr.beløp)
        }
        return GUnit(BigDecimal(0))
    }
}
