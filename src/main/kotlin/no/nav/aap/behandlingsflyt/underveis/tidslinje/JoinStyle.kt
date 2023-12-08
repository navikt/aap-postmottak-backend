package no.nav.aap.behandlingsflyt.underveis.tidslinje

enum class JoinStyle {
    /**
     * Ene eller andre har verdi.
     */
    CROSS_JOIN {
        override fun accept(harLhs: Boolean, harRhs: Boolean): Boolean {
            return harLhs || harRhs
        }
    },

    /**
     * kun venstre tidsserie.
     */
    DISJOINT {
        override fun accept(harLhs: Boolean, harRhs: Boolean): Boolean {
            return harLhs && !harRhs
        }
    },

    /**
     * kun dersom begge tidsserier har verdi.
     */
    INNER_JOIN {
        override fun accept(harLhs: Boolean, harRhs: Boolean): Boolean {
            return harLhs && harRhs
        }
    },

    /**
     * alltid venstre tidsserie (LHS), høyre (RHS) kun med verdi dersom matcher. Combinator funksjon må hensyn ta
     * nulls for RHS.
     */
    LEFT_JOIN {
        override fun accept(harLhs: Boolean, harRhs: Boolean): Boolean {
            return harLhs
        }
    },

    /**
     * alltid høyre side (RHS), venstre kun med verdi dersom matcher. Combinator funksjon må hensyn ta nulls for
     * LHS.
     */
    RIGHT_JOIN {
        override fun accept(harLhs: Boolean, harRhs: Boolean): Boolean {
            return harRhs
        }
    };

    abstract fun accept(harLhs: Boolean, harRhs: Boolean): Boolean
}