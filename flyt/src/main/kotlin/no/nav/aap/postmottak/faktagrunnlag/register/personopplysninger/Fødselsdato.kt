package no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger

import java.time.LocalDate

data class Fødselsdato(private val dato: LocalDate) {

    init {
        if (dato.isAfter(LocalDate.now())) throw IllegalArgumentException("Kan ikke sette fødselsdato inn i fremtiden")
    }

    fun toLocalDate(): LocalDate {
        return dato
    }
}
