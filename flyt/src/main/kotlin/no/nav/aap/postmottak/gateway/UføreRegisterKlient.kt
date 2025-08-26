package no.nav.aap.postmottak.gateway

import no.nav.aap.postmottak.PrometheusProvider

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.error.IkkeFunnetException
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.verdityper.Prosent
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * @param uforegrad Uføregrad i prosent. `null` om personen er registrert i systemet, men ikke har uføregrad.
 */

data class UførePeriode(
    val uforegradFom: LocalDate? = null,
    val uforegradTom: LocalDate? = null,
    val uforegrad: Int,
    val uforetidspunkt: LocalDate? = null,
    val virkningstidspunkt: LocalDate
)

class UføreRegisterKlient() : UføreRegisterGateway {
    private val log = LoggerFactory.getLogger(javaClass)
    private val url = URI.create(requiredConfigForKey("integrasjon.pesys.url"))
    private val config = ClientConfig(scope = requiredConfigForKey("integrasjon.pesys.scope"))

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )

    companion object : Factory<UføreRegisterKlient> {
        override fun konstruer(): UføreRegisterKlient {
            return UføreRegisterKlient()
        }
    }

    private fun queryMedHistorikk(uføreRequest: UføreRequest): UføreHistorikkRespons? {
        val httpRequest = PostRequest(
            additionalHeaders = listOf(
                Header("fnr", uføreRequest.fnr),
                Header("Nav-Consumer-Id", "aap-behandlingsflyt"),
                Header("Accept", "application/json")
            ),
            body = uføreRequest
        )

        val uri = url.resolve("pen/api/uforetrygd/uforehistorikk/perioder")
        try {
            return client.post(
                uri = uri,
                request = httpRequest
            )
        } catch (e: IkkeFunnetException) {
            log.info("Fant ikke person i PESYS. Returnerer null. Message: ${e.message}")
            return null
        }
    }

    override fun innhentPerioder(fnr: String, fraDato: LocalDate): List<Uføre> {
        val datoString = fraDato.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val request = UføreRequest(fnr, datoString)
        val uføreResponse = queryMedHistorikk(request)
        val uføreperioder = uføreResponse?.uforeperioder ?: emptyList()

        return uføreperioder.map {
            Uføre(
                virkningstidspunkt = it.virkningstidspunkt,
                uføregrad = Prosent(it.uforegrad)
            )
        }
    }
}