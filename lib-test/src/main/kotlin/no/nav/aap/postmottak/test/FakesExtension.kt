package no.nav.aap.postmottak.test

import org.junit.jupiter.api.extension.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class FakesExtension : BeforeAllCallback,
    BeforeEachCallback, ParameterResolver {

    private val log: Logger = LoggerFactory.getLogger(FakeServers::class.java)

    init {
        Thread.currentThread().setUncaughtExceptionHandler { _, e -> log.error("Uhåndtert feil", e) }
    }

    private val fakeServers = FakeServers()

    override fun beforeAll(context: ExtensionContext) {
        FakeServers().start()
    }

    override fun beforeEach(context: ExtensionContext) {

    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        return parameterContext.parameter.type == FakePersoner::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return fakeServers.fakePersoner
    }
}