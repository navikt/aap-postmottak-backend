package no.nav.aap.behandlingsflyt.beregning

import no.nav.aap.verdityper.GUnit

class Grunnlag11_19(
    private val grunnlaget: GUnit
) : Beregningsgrunnlag {

    override fun grunnlaget(): GUnit {
        return grunnlaget
    }

    override fun toString(): String {
        return "Grunnlag11_19(grunnlaget=$grunnlaget)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Grunnlag11_19

        return grunnlaget == other.grunnlaget
    }

    override fun hashCode(): Int {
        return grunnlaget.hashCode()
    }
}
