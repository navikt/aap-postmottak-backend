package no.nav.aap.postmottak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost

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
            Tag.of("filtype", journalpost.finnOriginal()?.filtype?.name ?: "UKJENT")
        )
    )

// TODO: Oppdater mappestruktur og bruk i AutomatiskJournalføringJobbUtfører og ManuellJournalføringJobbUtfører 
fun MeterRegistry.journalføringCounter(automatisk: Boolean) = 
    this.counter("arena_journalfoering", listOf(Tag.of("automatisk", automatisk.toString())))

enum class Fagsystem {
    arena,
    kelvin
}