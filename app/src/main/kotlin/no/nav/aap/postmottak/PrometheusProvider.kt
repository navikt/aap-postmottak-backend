package no.nav.aap.postmottak

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

class PrometheusProvider {
    companion object {
        lateinit var prometheus: PrometheusMeterRegistry
    }
}