package no.nav.aap

import io.mockk.mockk
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import org.junit.jupiter.api.BeforeAll

interface WithDependencies {
    companion object {
        val repositoryRegistry = postgresRepositoryRegistry
        
        @JvmStatic
        @BeforeAll
        fun setup() {
            PrometheusProvider.prometheus = mockk(relaxed = true)
        }
    }
}