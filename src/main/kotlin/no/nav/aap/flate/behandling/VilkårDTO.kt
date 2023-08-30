package no.nav.aap.flate.behandling

import no.nav.aap.domene.behandling.Vilkårstype

data class VilkårDTO(val vilkårstype: Vilkårstype, val perioder: List<VilkårsperiodeDTO>)
