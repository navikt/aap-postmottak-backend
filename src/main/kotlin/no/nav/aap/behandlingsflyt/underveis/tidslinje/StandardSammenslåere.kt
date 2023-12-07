package no.nav.aap.behandlingsflyt.underveis.tidslinje

import no.nav.aap.behandlingsflyt.Periode
import no.nav.aap.behandlingsflyt.faktagrunnlag.inntekt.Beløp
import java.math.BigDecimal

class PrioriterHøyreSide<T> : SegmentSammenslåer<T, T, T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        if (høyreSegment == null) {
            return Segment(periode, venstreSegment?.verdi)
        }
        return Segment(periode, høyreSegment.verdi)
    }
}

class PrioriteVenstreSide<T> : SegmentSammenslåer<T, T, T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        if (venstreSegment == null) {
            return Segment(periode, høyreSegment?.verdi)
        }
        return Segment(periode, venstreSegment.verdi)
    }
}

class KunHøyre<T> : SegmentSammenslåer<T, T, T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        return Segment(periode, høyreSegment?.verdi)
    }
}

class KunVenstre<T> : SegmentSammenslåer<T, T, T> {
    override fun sammenslå(periode: Periode, venstreSegment: Segment<T>?, høyreSegment: Segment<T>?): Segment<T> {
        return Segment(periode, venstreSegment?.verdi)
    }
}

class Summer : SegmentSammenslåer<Beløp, Beløp, Beløp> {
    override fun sammenslå(
        periode: Periode,
        venstreSegment: Segment<Beløp>?,
        høyreSegment: Segment<Beløp>?
    ): Segment<Beløp> {
        val høyreVerdi = høyreSegment?.verdi ?: Beløp(BigDecimal.ZERO)
        val venstreVerdi = venstreSegment?.verdi ?: Beløp(BigDecimal.ZERO)

        return Segment(periode, høyreVerdi.pluss(venstreVerdi))
    }

}