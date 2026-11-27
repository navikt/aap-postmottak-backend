package no.nav.aap.postmottak.under18

import java.time.LocalDate

enum class Under18Aldersgruppe(val påkrevdSøknadssituasjon: Under18Søknadssituasjon?) {
    FOR_UNG(Under18Søknadssituasjon.SØKER_FOR_UNG),
    SAMTYKKE(null),
    FYLT_18(Under18Søknadssituasjon.SØKER_FYLT_18);

    companion object {
        fun vurder(fødselsdato: LocalDate, dato: LocalDate, fylt17ÅrOg9Måneder: Boolean? = null): Under18Aldersgruppe = when {
            !dato.isBefore(fødselsdato.plusYears(18)) -> FYLT_18
            fylt17ÅrOg9Måneder ?: !dato.isBefore(fødselsdato.plusYears(17).plusMonths(9)) -> SAMTYKKE
            else -> FOR_UNG
        }
    }
}
