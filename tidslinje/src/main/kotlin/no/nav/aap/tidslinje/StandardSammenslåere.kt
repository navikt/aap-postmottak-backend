package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Beløp
import java.math.BigDecimal

object StandardSammenslåere {
    fun summerer(): JoinStyle.CROSS_JOIN<Beløp?, Beløp?, Beløp> {
        return JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
            val høyreVerdi = høyreSegment ?: Beløp(BigDecimal.ZERO)
            val venstreVerdi = venstreSegment ?: Beløp(BigDecimal.ZERO)

            Segment(periode, høyreVerdi.pluss(venstreVerdi))
        }
    }

    fun <T : Any> prioriterHøyreSide(): JoinStyle.INNER_JOIN<T, T, T> {
        return JoinStyle.INNER_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment)
        }
    }

    fun <T> prioriterHøyreSideCrossJoin(): JoinStyle.CROSS_JOIN<T, T, T> {
        return JoinStyle.CROSS_JOIN { periode, venstre, høyre ->
            if (høyre != null) return@CROSS_JOIN Segment(periode, høyre)
            if (venstre == null) return@CROSS_JOIN null
            Segment(periode, venstre)
        }
    }

    fun <T> prioriterVenstreSide(): JoinStyle.CROSS_JOIN<T, T, T> {
        return JoinStyle.CROSS_JOIN { periode, venstreSegment, høyreSegment ->
            if (venstreSegment == null) {
                if (høyreSegment != null) {
                    Segment(periode, høyreSegment)
                } else {
                    null
                }
            } else {
                Segment(periode, venstreSegment)
            }
        }
    }

    fun <T, E> kunVenstre(): JoinStyle.INNER_JOIN<T, E, T> {
        return JoinStyle.INNER_JOIN { periode, venstreSegment, _ ->
            Segment(periode, venstreSegment)
        }
    }

    fun <T> kunHøyre(): JoinStyle.LEFT_JOIN<Any?, T, T> {
        return JoinStyle.LEFT_JOIN { periode, _, høyreSegment ->
            if (høyreSegment == null) {
                null
            } else {
                Segment(periode, høyreSegment)
            }
        }
    }

    fun <T> kunHøyreRightJoin(): JoinStyle.RIGHT_JOIN<Any?, T, T> {
        return JoinStyle.RIGHT_JOIN { periode, _, høyreSegment ->
            Segment(periode, høyreSegment)
        }
    }
}