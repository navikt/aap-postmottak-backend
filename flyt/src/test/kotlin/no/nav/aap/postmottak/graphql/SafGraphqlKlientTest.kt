package no.nav.aap.postmottak.graphql

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.gateway.GatewayRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tilJournalpost
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*


@Fakes
class SafGraphqlKlientTest {
    @BeforeEach
    fun setup() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        GatewayRegistry.register(SafGraphqlClientCredentialsClient::class)
    }

    @Test
    fun hentJournalpost() {
        val person =  Person(123, identifikator = UUID.randomUUID(), identer = listOf(Ident("12345678")))
        val test = GatewayProvider.provide(JournalpostGateway::class).hentJournalpost(JournalpostId(1)).tilJournalpost(person)

        assertThat(test.journalpostId).isEqualTo(JournalpostId(1))
    }
}