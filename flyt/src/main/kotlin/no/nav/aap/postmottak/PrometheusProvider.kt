package no.nav.aap.postmottak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Variantformat

class PrometheusProvider {
    companion object {
        lateinit var prometheus: PrometheusMeterRegistry
    }
}

fun MeterRegistry.fordelingsCounter(system: Fagsystem): Counter =
    this.counter("fordeling_videresend", listOf(Tag.of("system", system.name)))

fun MeterRegistry.journalpostCounter(journalpost: Journalpost) =
    this.counter(
        "journalpost",
        listOf(
            Tag.of("brevkode", journalpost.hoveddokumentbrevkode),
            Tag.of("filtype", journalpost.finnOriginal()?.finnFiltype(Variantformat.ORIGINAL)?.name ?: "UKJENT")
        )
    )

fun MeterRegistry.journalføringCounter(type: JournalføringsType, enhet: NavEnhet? = null): Counter {
    val tags = mutableListOf(Tag.of("type", type.name))
    if (enhet != null) {
        tags += Tag.of("enhet", enhet.tilMonitoreringEnhetsgruppe())
    }
    return this.counter(
        "arena_journalfoering", tags
    )
}

enum class Fagsystem {
    arena,
    kelvin
}

enum class JournalføringsType {
    automatisk,
    jfr,
    fdr
}

private fun NavEnhet.tilMonitoreringEnhetsgruppe(): String {
    return when {
        this == "0393" -> "Oppfølging utland"
        this == "4491" -> "NAY"
        this == "4402" -> "NAY utland"
        this == "4260" -> "Klageinstans AAP"
        this.startsWith("02") -> "Øst-Viken"
        this.startsWith("03") -> "Oslo"
        this.startsWith("04") -> "Innlandet"
        this.startsWith("06") -> "Vest-Viken"
        this.startsWith("08") -> "Vestfold og Telemark"
        this.startsWith("10") -> "Agder"
        this.startsWith("11") -> "Rogaland"
        this.startsWith("12") -> "Vestland"
        this.startsWith("15") -> "Møre og Romsdal"
        this.startsWith("18") -> "Nordland"
        this.startsWith("19") -> "Troms og Finnmark"
        this.startsWith("57") -> "Trøndelag"
        else -> "Annet"
    }
}