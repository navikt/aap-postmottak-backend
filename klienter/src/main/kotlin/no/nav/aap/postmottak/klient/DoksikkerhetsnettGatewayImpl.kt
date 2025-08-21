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
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import java.net.URI

/**
 * Dokumentasjon:
 * https://confluence.adeo.no/spaces/BOA/pages/675059220/finnMottatteJournalposter
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
        return requireNotNull(client.get<List<JournalpostFraDoksikkerhetsnett>>(resolvedUrl, GetRequest()))
    }
}