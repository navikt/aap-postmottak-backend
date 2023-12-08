package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Segment
import no.nav.aap.behandlingsflyt.underveis.tidslinje.SegmentSammenslåer

class LeggTilGraderingPåVurderingerSammenslåer : SegmentSammenslåer<Vurdering, Prosent, Vurdering> {
    override fun sammenslå(
        periode: Periode,
        venstreSegment: Segment<Vurdering>?,
        høyreSegment: Segment<Prosent>?
    ): Segment<Vurdering> {
        return if (venstreSegment?.verdi == null && høyreSegment?.verdi != null) {
            val nyVurdering = Vurdering()
            nyVurdering.leggTilGradering(høyreSegment.verdi)
            Segment(periode, nyVurdering)
        } else {
            val vurdering = venstreSegment?.verdi
            if (høyreSegment?.verdi != null) {
                vurdering?.leggTilGradering(høyreSegment.verdi)
            }
            Segment(periode, vurdering)
        }
    }
}