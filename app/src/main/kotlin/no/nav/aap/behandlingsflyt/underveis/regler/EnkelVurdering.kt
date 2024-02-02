package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Utfall
import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.vilkårsresultat.Vilkårtype

data class EnkelVurdering(val vilkår: Vilkårtype, val utfall: Utfall)
