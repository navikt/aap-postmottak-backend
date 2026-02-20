package no.nav.aap.postmottak.graphql

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


@Fakes
class SafGraphqlKlientTest {
    private val gatewayProvider = createGatewayProvider {
        register<SafGraphqlClientCredentialsClient>()
    }
    
    @BeforeEach
    fun setup() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    @Test
    fun hentJournalpost() {
        val test = gatewayProvider.provide(JournalpostGateway::class).hentJournalpost(JournalpostId(1))

        assertThat(test.journalpostId).isEqualTo(1L)
    }
}