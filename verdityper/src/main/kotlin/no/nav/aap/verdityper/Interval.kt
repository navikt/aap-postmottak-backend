package no.nav.aap.verdityper

import java.time.LocalDateTime

class Interval(val fom: LocalDateTime, val tom: LocalDateTime) : Comparable<Interval> {

    init {
        require(fom <= tom) { "tom($tom) er før fom($fom)" }
    }

    fun overlapper(other: Interval): Boolean {
        return this.fom <= other.tom && other.fom <= this.tom
    }

    fun inneholder(dato: LocalDateTime): Boolean {
        return dato in fom..tom
    }


    fun overlapp(other: Interval): Interval? {
        return if (!this.overlapper(other)) {
            null
        } else if (this == other) {
            this
        } else {
            Interval(maxOf(fom, other.fom), minOf(tom, other.tom))
        }
    }

    override fun compareTo(other: Interval): Int {
        val compareFom = fom.compareTo(other.fom)

        if (compareFom != 0) {
            return compareFom
        }

        return tom.compareTo(other.tom)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Interval

        if (fom != other.fom) return false
        return tom == other.tom
    }

    override fun hashCode(): Int {
        var result = fom.hashCode()
        result = 31 * result + tom.hashCode()
        return result
    }

    override fun toString(): String {
        return "Interval(fom=$fom, tom=$tom)"
    }
}
