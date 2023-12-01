package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.adapter.InntektPerÅr

class YrkesskadeBeregning(
    private val grunnlag11_19: Grunnlag11_19,
    //TODO: Skal antattÅrligInntekt begrenses til 6G i det hele tatt?...
    private val antattÅrligInntekt: InntektPerÅr,
    private val andelAvNedsettelsenSomSkyldesYrkesskaden: Prosent
) {
    private companion object {
        private val TERSKELVERDI_FOR_YRKESSKADE = Prosent.`70_PROSENT`
    }

    fun beregnYrkesskaden(): GrunnlagYrkesskade {
        val andelForBeregning = andelAvNedsettelsenSomSkyldesYrkesskaden.justertFor(TERSKELVERDI_FOR_YRKESSKADE)
        val grunnlagsandelSomSkyldesYrkesskade = grunnlag11_19.grunnlaget()
        //TODO: ...og skal antattÅrligInntektGUnits begrenses til 6G...
        val antattÅrligInntektGUnits = antattÅrligInntekt.gUnit()

        val maxOf = maxOf(grunnlagsandelSomSkyldesYrkesskade, antattÅrligInntektGUnits)
        //TODO: ...eller skal andelSomSkyldesYrkesskade begrenses til 6G
        val andelSomSkyldesYrkesskade = maxOf.multiplisert(andelForBeregning)
        val andelSomIkkeSkyldesYrkesskade = grunnlag11_19.grunnlaget().multiplisert(andelForBeregning.kompliment())

        val grunnlag = andelSomSkyldesYrkesskade.pluss(andelSomIkkeSkyldesYrkesskade)

        return GrunnlagYrkesskade(
            grunnlaget = grunnlag,
            grunnlag11_19 = grunnlag11_19
        )
    }
}
