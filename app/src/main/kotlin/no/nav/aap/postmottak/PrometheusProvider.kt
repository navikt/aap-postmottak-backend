package no.nav.aap.postmottak

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class PrometheusProvider {
    companion object {
        lateinit var prometheus: PrometheusMeterRegistry
    }
}

enum class Fagsystem {
    arena,
    kelvin
}
fun MeterRegistry.fordelingsCounter(system: Fagsystem): Counter =
    this.counter("fordeling_videresend", listOf(Tag.of("system", system.name)))