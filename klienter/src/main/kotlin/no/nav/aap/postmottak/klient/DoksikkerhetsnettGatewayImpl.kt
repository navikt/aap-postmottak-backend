package no.nav.aap.postmottak.klient

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.PrometheusProvider.Companion.prometheus
import no.nav.aap.postmottak.gateway.DoksikkerhetsnettGateway
import no.nav.aap.postmottak.gateway.JournalpostFraDoksikkerhetsnett
import java.net.URI

/**
 * Dokumentasjon:
 * https://confluence.adeo.no/spaces/BOA/pages/675059220/finnMottatteJournalposter
 *
 * Og Swagger her: https://dokarkiv.dev.intern.nav.no/swagger-ui/index.html
 */
class DoksikkerhetsnettGatewayImpl : DoksikkerhetsnettGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))
    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.joark.scope"),
    )

    companion object : Factory<DoksikkerhetsnettGateway> {
        override fun konstruer(): DoksikkerhetsnettGateway {
            return DoksikkerhetsnettGatewayImpl()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = prometheus
    )

    override fun finnMottatteJournalposterEldreEnn(antallDagerGamle: Int): List<JournalpostFraDoksikkerhetsnett> {
        val resolvedUrl =
            url.resolve("/rest/journalpostapi/v1/finnMottatteJournalposter?tema=AAP&antallDagerGamle=$antallDagerGamle")
        return requireNotNull(client.get<FinnMottatteJournalposterResponse>(resolvedUrl, GetRequest())).journalposter
    }
}

// Respons her: https://github.com/navikt/dokarkiv/blob/ac7f895d29c458474cf012e1aadeb517951608f1/journalpost/src/main/java/no/nav/dokarkiv/journalpost/v1/api/finnMottatteJournalposter/FinnMottatteJournalposterResponse.java
private data class FinnMottatteJournalposterResponse(val journalposter: List<JournalpostFraDoksikkerhetsnett>)