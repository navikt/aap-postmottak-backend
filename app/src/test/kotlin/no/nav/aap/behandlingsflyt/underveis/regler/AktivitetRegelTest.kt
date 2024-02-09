package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.underveis.UnderveisAvslagsårsak
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.vilkårsresultat.Utfall
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.dokument.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class AktivitetRegelTest {
    private val regel = AktivitetRegel()

    @Test
    fun `Meldeplikt overholdt ved innsendt på fastsatt dag`() {
        val fom = LocalDate.now().minusMonths(3)
        val rettighetsperiode = Periode(fom, fom.plusWeeks(6).minusDays(1))
        val input = UnderveisInput(
            rettighetsperiode = rettighetsperiode,
            relevanteVilkår = listOf(),
            opptrappingPerioder = listOf(),
            pliktkort = listOf(),
            innsendingsTidspunkt = mapOf(
                Pair(fom.plusDays(13), JournalpostId("1")),
                Pair(fom.plusDays(27), JournalpostId("2")),
                Pair(fom.plusDays(41), JournalpostId("3"))
            )
        )

        val vurdertTidslinje = regel.vurder(input, Tidslinje())

        assertThat(vurdertTidslinje.segmenter()).allMatch { it.verdi?.meldeplikUtfall() == Utfall.OPPFYLT }
    }

    @Test
    fun `Meldeplikt ikke overholdt ved innsendt på fastsatt dag`() {
        val fom = LocalDate.now().minusMonths(3)
        val rettighetsperiode = Periode(fom, fom.plusWeeks(6).minusDays(1))
        val input = UnderveisInput(
            rettighetsperiode = rettighetsperiode,
            relevanteVilkår = listOf(),
            opptrappingPerioder = listOf(),
            pliktkort = listOf(),
            innsendingsTidspunkt = mapOf(
                Pair(fom.plusDays(13), JournalpostId("1")),
                Pair(fom.plusDays(33), JournalpostId("2"))
            )
        )

        val vurdertTidslinje = regel.vurder(input, Tidslinje())

        val avslåttesegmenter = vurdertTidslinje.segmenter().filter { it.verdi?.meldeplikUtfall() != Utfall.OPPFYLT }
        assertThat(avslåttesegmenter).hasSize(2)
        assertThat(avslåttesegmenter).allMatch { it.verdi?.meldeplikAvslagsårsak() == UnderveisAvslagsårsak.IKKE_OVERHOLDT_MELDEPLIKT_SANKSJON }
    }
}