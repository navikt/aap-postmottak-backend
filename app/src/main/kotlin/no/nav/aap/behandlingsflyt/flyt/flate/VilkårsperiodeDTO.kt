package no.nav.aap.behandlingsflyt.flyt.flate

import no.nav.aap.behandlingsflyt.flyt.vilkår.Avslagsårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Innvilgelsesårsak
import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.verdityper.Periode

data class VilkårsperiodeDTO(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val begrunnelse: String?,
    val avslagsårsak: Avslagsårsak?,
    val innvilgelsesårsak: Innvilgelsesårsak?
)
