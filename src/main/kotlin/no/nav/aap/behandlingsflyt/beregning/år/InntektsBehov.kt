package no.nav.aap.behandlingsflyt.beregning.år

import java.time.LocalDate
import java.time.Year

class InntektsBehov {

    fun utled(input: Input): Set<Year> {

        return input.datoerForInnhenting()
            .flatMap { treÅrForutFor(it) }
            .toSortedSet()
    }

    private fun treÅrForutFor(nedsettelsesDato: LocalDate): Set<Year> {
        val nedsettelsesÅr = Year.of(nedsettelsesDato.year)

        return setOf(nedsettelsesÅr.minusYears(3), nedsettelsesÅr.minusYears(2), nedsettelsesÅr.minusYears(1))
    }
}