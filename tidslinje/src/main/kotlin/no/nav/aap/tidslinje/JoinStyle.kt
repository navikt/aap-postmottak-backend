package no.nav.aap.tidslinje

import no.nav.aap.verdityper.Periode

interface JoinStyle<in VENSTRE, in HØYRE:Any?, RETUR, out SRETUR : Segment<RETUR>?> {
    fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): SRETUR?

    /**
     * Ene eller andre har verdi.
     */
    class CROSS_JOIN<VENSTRE, HØYRE, RETUR, SRETUR : Segment<RETUR>>(
        private val kombinerer: (Periode, VENSTRE?, HØYRE?) -> SRETUR?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR, SRETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): SRETUR? {
            if (venstre == null && høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * kun venstre tidsserie.
     */
    class DISJOINT<VENSTRE, HØYRE, RETUR, SRETUR : Segment<RETUR>>(
        private val kombinerer: (Periode, VENSTRE & Any) -> SRETUR
    ) : JoinStyle<VENSTRE, HØYRE, RETUR, SRETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): SRETUR? {
            if (venstre == null || høyre != null) return null
            return this.kombinerer(periode, venstre)
        }
    }

    /**
     * kun dersom begge tidsserier har verdi.
     */
    class INNER_JOIN<VENSTRE, HØYRE, RETUR, SRETUR : Segment<RETUR>>(
        private val kombinerer: (Periode, VENSTRE & Any, HØYRE & Any) -> SRETUR?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR, SRETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): SRETUR? {
            if (venstre == null || høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
     * nulls for RHS.
     */
    class LEFT_JOIN<VENSTRE, HØYRE, RETUR, SRETUR : Segment<RETUR>>(
        private val kombinerer: (Periode, VENSTRE & Any, HØYRE?) -> SRETUR?
    ) : JoinStyle<VENSTRE, HØYRE, RETUR, SRETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): SRETUR? {
            if (venstre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }

    /**
     * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
     * LHS.
     */
    class RIGHT_JOIN<VENSTRE, HØYRE, RETUR, SRETUR : Segment<RETUR>>(
        private val kombinerer: (Periode, VENSTRE?, HØYRE & Any) -> SRETUR
    ) : JoinStyle<VENSTRE, HØYRE, RETUR, SRETUR> {
        override fun kombiner(periode: Periode, venstre: VENSTRE?, høyre: HØYRE?): SRETUR? {
            if (høyre == null) return null
            return this.kombinerer(periode, venstre, høyre)
        }
    }
}
