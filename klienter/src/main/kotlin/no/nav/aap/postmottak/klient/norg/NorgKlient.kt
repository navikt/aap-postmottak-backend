package no.nav.aap.postmottak.klient.norg

import no.nav.aap.fordeler.Diskresjonskode
import no.nav.aap.fordeler.NorgGateway
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.get
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.GetRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.NoTokenTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Behandlingstema
import no.nav.aap.postmottak.klient.gosysoppgave.NavEnhet
import org.slf4j.LoggerFactory
import java.net.URI

data class Enhet(val enhetNr: String)

data class FinnNavenhetRequest(
    val geografiskOmraade: String?,
    val skjermet: Boolean = false,
    val diskresjonskode: Diskresjonskode,
    val tema: String = "AAP",
    val behandlingstema: String = Behandlingstema.AAP.kode,
    val behandlingstype: String? = null,
    val oppgavetype: String? = null
)

class NorgKlient : NorgGateway {

    companion object : Factory<NorgKlient> {
        override fun konstruer(): NorgKlient {
            return NorgKlient()
        }
    }

    private val log = LoggerFactory.getLogger(NorgKlient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.norg.url"))
    private val config = ClientConfig()
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = NoTokenTokenProvider(),
        prometheus = PrometheusProvider.prometheus
    )

    override fun hentAktiveEnheter(): List<NavEnhet> {
        val aktiveEnheterUrl = url.resolve("norg2/api/v1/enhet")
        val request = GetRequest()

        return client.get<List<Enhet>>(aktiveEnheterUrl, request)?.map { it.enhetNr }
            ?: error("Feil i response fra norg")
    }

    override fun finnArbeidsfordelingsEnhet(
        geografiskTilknytning: String?,
        erNavansatt: Boolean,
        diskresjonskode: Diskresjonskode,
        behandlingstema: String,
        behandlingstype: String?,
        oppgavetype: Oppgavetype?
    ): NavEnhet? {
        log.info("Finner enhet for $geografiskTilknytning")
        val finnEnhetUrl = url.resolve("norg2/api/v1/arbeidsfordeling/enheter/bestmatch")
        val request = PostRequest(
            FinnNavenhetRequest(
                geografiskTilknytning,
                erNavansatt,
                diskresjonskode,
                behandlingstema = behandlingstema,
                behandlingstype = behandlingstype,
                oppgavetype = oppgavetype?.verdi
            )
        )

        return client.post<FinnNavenhetRequest, List<Enhet>>(finnEnhetUrl, request)
            .let { response -> response?.firstOrNull()?.enhetNr }
            .also { if (it == null) log.info("Fant ingen enhet") }
    }
}