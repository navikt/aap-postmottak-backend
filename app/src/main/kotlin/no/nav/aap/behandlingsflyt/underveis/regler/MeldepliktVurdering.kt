package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisAvslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.verdityper.Periode

data class MeldepliktVurdering(
    val meldeperiode: Periode,
    val utfall: Utfall,
    val avslagsårsak: UnderveisAvslagsårsak? = null
)
