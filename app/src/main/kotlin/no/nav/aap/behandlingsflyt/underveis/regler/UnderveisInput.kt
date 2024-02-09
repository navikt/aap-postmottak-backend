package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Vilkår
import no.nav.aap.behandlingsflyt.faktagrunnlag.dokument.arbeid.Pliktkort
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import java.time.LocalDate

data class UnderveisInput(
    val rettighetsperiode: Periode,
    val relevanteVilkår: List<Vilkår>,
    val opptrappingPerioder: List<Periode>,
    val pliktkort: List<Pliktkort>,
    val innsendingsTidspunkt: Map<LocalDate, JournalpostId>
)
