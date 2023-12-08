package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype

class Vurdering {
    private val vurderinger: HashMap<Vilkårtype, Utfall> = HashMap()

    fun leggTilVurdering(vilkårtype: Vilkårtype, utfall: Utfall) {
        vurderinger[vilkårtype] = utfall
    }

    fun vurderinger(): Map<Vilkårtype, Utfall> {
        return vurderinger
    }

    fun harRett():Boolean {
        return vurderinger.isNotEmpty() && vurderinger.none { it.value == Utfall.IKKE_OPPFYLT }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Vurdering

        return vurderinger == other.vurderinger
    }

    override fun hashCode(): Int {
        return vurderinger.hashCode()
    }

}
