package no.nav.aap.postmottak.klient.arena

import no.nav.aap.fordeler.NavEnhet
import no.nav.aap.fordeler.VeilarbarenaGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.PrometheusProvider
import org.slf4j.LoggerFactory
import java.net.URI

private data class HentOppfølgingsenhetRequest(
    val fnr: String
)

private data class HentOppfølgingsenhetResponse(
    val oppfolgingsenhet: String?
)

class VeilarbarenaKlient : VeilarbarenaGateway {
    private val log = LoggerFactory.getLogger(VeilarbarenaKlient::class.java)
    companion object : Factory<VeilarbarenaKlient> {
        override fun konstruer(): VeilarbarenaKlient {
            return VeilarbarenaKlient()
        }
    }


    private val url = URI.create(requiredConfigForKey("integrasjon.veilarbarena.url"))

    private val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.veilarbarena.scope"),
    )

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )

    override fun hentOppfølgingsenhet(personident: String): NavEnhet? {
        val hentStatusUrl = url.resolve("/veilarbarena/api/v2/arena/hent-status")
        val request = PostRequest(
            body = HentOppfølgingsenhetRequest(personident),
            additionalHeaders = listOf(
                Header("forceSync", "true"),
                Header("Nav-Consumer-Id", "aap-postmottak-backend"),
            )
        )
        val resp = try {
            client.post<HentOppfølgingsenhetRequest, HentOppfølgingsenhetResponse?>(hentStatusUrl, request)
        } catch (_: IkkeFunnetException) {
            // Tjenesten returner 404 dersom det ikke finnes noen oppfølgingsenhet for oppgitt fnr
            return null
        }
        
        if (resp?.oppfolgingsenhet != null) {
            log.info("Oppfølgingsenhet.length: ${resp.oppfolgingsenhet.length}")
        }
        
        return resp?.oppfolgingsenhet
    }

}