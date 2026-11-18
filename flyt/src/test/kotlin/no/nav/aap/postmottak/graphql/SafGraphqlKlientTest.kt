package no.nav.aap.postmottak.graphql

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostSak
import no.nav.aap.postmottak.gateway.Sak
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.klient.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.fakes.hoveddokument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID


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

    @Test
    fun `henter tilknyttede journalposter gjennom registrerte gatewayer og HTTP-fake`() {
        val dokument = hoveddokument("NAV 11-13.05", "Test").copy(
            dokumentInfoId = DokumentInfoId(UUID.randomUUID().toString())
        )
        val kilde = TestJournalposter.leggTil { dokumenter = listOf(dokument) }
        val tilknyttet = TestJournalposter.leggTil {
            dokumenter = listOf(dokument)
            fagsak = JournalpostSak(fagsakId = "SAK1")
        }

        val resultat = gatewayProvider.provide<JournalpostGateway>()
            .hentTilknyttedeJournalposter(dokument.dokumentInfoId)
        assertThat(resultat.map { it.journalpostId })
            .containsExactlyInAnyOrder(kilde.journalpostId.toString(), tilknyttet.journalpostId.toString())
        assertThat(resultat.single { it.journalpostId == tilknyttet.journalpostId.toString() }.sak)
            .isEqualTo(Sak("SAK1", "KELVIN", Sakstype.FAGSAK))
    }
}