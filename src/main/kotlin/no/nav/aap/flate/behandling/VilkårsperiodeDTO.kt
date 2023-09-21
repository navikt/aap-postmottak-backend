package no.nav.aap.flate.behandling

import no.nav.aap.domene.Periode
import no.nav.aap.domene.behandling.Avslagsårsak
import no.nav.aap.domene.behandling.Utfall

data class VilkårsperiodeDTO(
    val periode: Periode,
    val utfall: Utfall,
    val manuellVurdering: Boolean,
    val begrunnelse: String?,
    val avslagsårsak: Avslagsårsak?
)
