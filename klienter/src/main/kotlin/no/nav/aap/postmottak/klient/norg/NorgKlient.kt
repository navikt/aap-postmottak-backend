package no.nav.aap.postmottak.klient.norg

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.klient.gosysoppgave.NavEnhet
import java.net.URI

data class NavEnhetResponse(val enheter: List<Enhet>)

data class Enhet(val enhetNr: String)

data class FinnNavenhetRequest(
    val geografiskOmraade: String?,
    val skjermet: Boolean = false,
    val diskresjonskode : Diskresjonskode
){
    val tema = "AAP"
}

enum class Diskresjonskode { SPFO, SPSF, ANY }

class NorgKlient {

    private val url = URI.create(requiredConfigForKey("integrasjon.norg.url"))
    private val config = ClientConfig()
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = NoTokenTokenProvider(),
    )

    fun hentAktiveEnheter(): List<NavEnhet> {
        val aktiveEnheterUrl = url.resolve("norg2/api/v1/enhet")
        val request = GetRequest()

        return client.get<NavEnhetResponse>(aktiveEnheterUrl, request)?.enheter?.map { it.enhetNr } ?: error("Feil i response fra norg")
    }

    fun finnEnhet(geografiskTilknyttning: String?, erNavansatt: Boolean, diskresjonskode: Diskresjonskode): NavEnhet {
        val finnEnhetUrl = url.resolve("norg2/api/v1/arbeidsfordeling/enheter/bestmatch")
        val request = PostRequest(
            FinnNavenhetRequest(geografiskTilknyttning, erNavansatt, diskresjonskode)
        )

        return client.post<FinnNavenhetRequest, Enhet>(finnEnhetUrl, request).let { response -> response?.enhetNr } ?: error("Feil i response fra norg")
    }

}