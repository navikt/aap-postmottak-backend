package no.nav.aap.postmottak.klient.ereg

import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.EnhetsregisterOrganisasjonResponse
import no.nav.aap.postmottak.gateway.EnhetsregisteretGateway
import no.nav.aap.postmottak.gateway.Organisasjonsnummer
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

class EREGKlient : EnhetsregisteretGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val url = URI.create(requiredConfigForKey("integrasjon.ereg.url") + "/api/v2/organisasjon")
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.ereg.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )

    companion object : Factory<EREGKlient> {
        override fun konstruer(): EREGKlient {
            return EREGKlient()
        }
    }

    override fun hentOrganisasjon(organisasjonsnummer: Organisasjonsnummer): EnhetsregisterOrganisasjonResponse? {
        val httpRequest = GetRequest(
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return try {
            client.get(
                uri = URI.create("$url/${organisasjonsnummer.value}"),
                request = httpRequest
            )
        } catch (e: IkkeFunnetException) {
            log.warn("Fant ikke organisasjon i EREG for orgnr ${organisasjonsnummer.value}. Fortsetter uten verdi.", e)
            null
        } catch (e: Exception) {
            log.warn("Feil ved henting av data fra EREG for orgnr ${organisasjonsnummer.value}. Fortsetter uten verdi.", e)
            null
        }
    }
}