package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.tidslinje.Segment
import no.nav.aap.verdityper.Periode

internal class SlåSammenVurderingerSammenslåer  {
     internal fun  sammenslå(
        periode: Periode,
        venstreSegment: Vurdering?,
        høyreSegment: EnkelVurdering?
    ): Segment<Vurdering> {
        val høyreSegmentVerdi = høyreSegment
        val venstreSegmentVerdi = venstreSegment

        return if (venstreSegmentVerdi == null && høyreSegmentVerdi != null) {
            var nyVurdering = Vurdering()
            nyVurdering = nyVurdering.leggTilVurdering(høyreSegmentVerdi.vilkår, høyreSegmentVerdi.utfall)
            Segment(periode, nyVurdering)
        } else {
            var vurdering = venstreSegmentVerdi ?: Vurdering()
            if (høyreSegmentVerdi != null) {
                vurdering = vurdering.leggTilVurdering(høyreSegmentVerdi.vilkår, høyreSegmentVerdi.utfall)
            }
            Segment(periode, vurdering)
        }
    }
}
