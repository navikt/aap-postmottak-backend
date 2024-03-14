package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Periode

interface JoinStyle<in VENSTRE, in HØYRE, RETUR> {
    fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): Segment<RETUR>?

    /**
     * Ene eller andre har verdi.
     */
    class CROSS_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, VENSTRE?, HØYRE?) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): Segment<RETUR>? {
            if (venstre == null && høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * kun venstre tidsserie.
     */
    class DISJOINT<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, VENSTRE & Any) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): Segment<RETUR>? {
            if (venstre == null || høyre != null) return null
            return this.kombinerer(periode, venstre)
        }
    }

    /**
     * kun dersom begge tidsserier har verdi.
     */
    class INNER_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, VENSTRE & Any, HØYRE & Any) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): Segment<RETUR>? {
            if (venstre == null || høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
     * nulls for RHS.
     */
    class LEFT_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, VENSTRE & Any, HØYRE?) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): Segment<RETUR>? {
            if (venstre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
     * LHS.
     */
    class RIGHT_JOIN<VENSTRE, HØYRE, RETUR>(
        private val kombinerer: (Periode, VENSTRE?, HØYRE & Any) -> Segment<RETUR>?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): Segment<RETUR>? {
            if (høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }
}
