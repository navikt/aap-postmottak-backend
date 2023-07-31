package no.nav.aap.domene.behandling.grunnlag.person

import java.time.LocalDate

class Fødselsdato(val dato: LocalDate) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Fødselsdato

        return dato == other.dato
    }

    override fun hashCode(): Int {
        return dato.hashCode()
    }
}
