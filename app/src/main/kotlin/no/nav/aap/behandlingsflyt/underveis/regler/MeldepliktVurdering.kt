package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisÅrsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId

data class MeldepliktVurdering(
    val journalpostId: JournalpostId?,
    val meldeperiode: Periode,
    val utfall: Utfall,
    val årsak: UnderveisÅrsak? = null
)
