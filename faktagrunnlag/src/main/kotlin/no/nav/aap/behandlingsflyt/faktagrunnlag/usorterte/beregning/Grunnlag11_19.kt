package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.beregning

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Faktagrunnlag
import no.nav.aap.verdityper.GUnit
import java.math.BigDecimal

class Grunnlag11_19(
    private val grunnlaget: GUnit
) : no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.beregning.Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    override fun faktagrunnlag(): Faktagrunnlag {
        return no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.beregning.Grunnlag11_19.Fakta(
            grunnlaget = grunnlaget.verdi()
        )
    }

    internal class Fakta(
        // FIXME: BigDecimal serialiseres til JSON på standardform
        val grunnlaget: BigDecimal
    ) : Faktagrunnlag

    override fun toString(): String {
        return "Grunnlag11_19(grunnlaget=$grunnlaget)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.beregning.Grunnlag11_19

        return grunnlaget == other.grunnlaget
    }

    override fun hashCode(): Int {
        return grunnlaget.hashCode()
    }
}
