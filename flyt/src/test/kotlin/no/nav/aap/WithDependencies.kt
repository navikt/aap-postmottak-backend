package no.nav.aap

import io.mockk.mockk
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import no.nav.aap.postmottak.test.fakes.InMemoryUnder18Repository
import org.junit.jupiter.api.BeforeAll

interface WithDependencies {
    companion object {
        // Fjern InMemoryUnder18Repository når Postgres-implementasjonen er registrert.
        val repositoryRegistry = postgresRepositoryRegistry.register<InMemoryUnder18Repository>()

        @JvmStatic
        @BeforeAll
        fun setup() {
            PrometheusProvider.prometheus = mockk(relaxed = true)
        }
    }
}