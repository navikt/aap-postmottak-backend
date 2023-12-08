package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.flyt.vilkår.Utfall
import no.nav.aap.behandlingsflyt.flyt.vilkår.Vilkårtype

data class EnkelVurdering(val vilkår: Vilkårtype, val utfall: Utfall)
