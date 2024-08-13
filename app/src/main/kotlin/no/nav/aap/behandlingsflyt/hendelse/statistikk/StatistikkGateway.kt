package no.nav.aap.behandlingsflyt.hendelse.statistikk

import no.nav.aap.httpclient.ClientConfig
import no.nav.aap.httpclient.RestClient
import no.nav.aap.httpclient.post
import no.nav.aap.httpclient.request.PostRequest
import no.nav.aap.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.requiredConfigForKey
import java.net.URI

class StatistikkGateway(restClient: RestClient<String>? = null) {
    // TODO: legg på auth mellom appene
    private val restClient = restClient ?: RestClient.withDefaultResponseHandler(
        config = ClientConfig(scope = requiredConfigForKey("integrasjon.statistikk.scope")),
        tokenProvider = ClientCredentialsTokenProvider
    )

    private val uri = URI.create(requiredConfigForKey("integrasjon.statistikk.url"))

    fun avgiStatistikk(hendelse: StatistikkHendelseDTO) {
        restClient.post<_, Unit>(uri = uri.resolve("/motta"), request = PostRequest(body = hendelse))
    }

    fun avsluttetBehandling(hendelse: AvsluttetBehandlingDTO) {
        restClient.post<_, Unit>(uri = uri.resolve("/avsluttetBehandling"), request = PostRequest(body = hendelse))
    }
}