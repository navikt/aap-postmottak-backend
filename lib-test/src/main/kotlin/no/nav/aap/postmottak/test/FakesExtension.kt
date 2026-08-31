package no.nav.aap.postmottak.test

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class FakesExtension : BeforeAllCallback {

    private val log: Logger = LoggerFactory.getLogger(javaClass)

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    override fun beforeAll(context: ExtensionContext) {
        FakeServers().start()
    }
}