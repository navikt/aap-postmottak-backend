package no.nav.aap.postmottak.gateway


import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.PrometheusProvider.Companion.prometheus
import java.net.URI
import java.time.LocalDate

/**
 * Henter alle ytelser i fpabakus
 */
class AbakusForeldrepengerGateway : ForeldrepengerGateway {
    private val url = URI.create(requiredConfigForKey("INTEGRASJON_FORELDREPENGER_URL") + "/hent-ytelse-vedtak")
    private val config = ClientConfig(scope = requiredConfigForKey("INTEGRASJON_FORELDREPENGER_SCOPE"))

    companion object : Factory<ForeldrepengerGateway> {
        override fun konstruer(): ForeldrepengerGateway {
            return AbakusForeldrepengerGateway()
        }
    }

    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = AzureM2MTokenProvider,
        prometheus = prometheus
    )

    private fun query(request: ForeldrepengerRequest): ForeldrepengerResponse {
        val httpRequest = PostRequest(
            body = request,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        val response: List<Ytelse> = requireNotNull(client.post(uri = url, request = httpRequest))
        return ForeldrepengerResponse(response)
    }

    override fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse {
        val result = query(request)
        return result
    }
}
interface ForeldrepengerGateway : Gateway {
    fun hentVedtakYtelseForPerson(request: ForeldrepengerRequest): ForeldrepengerResponse
}

data class ForeldrepengerRequest(
    val ident: Aktør,
    val periode: Periode,
)

data class ForeldrepengerResponse(
    val ytelser: List<Ytelse>
)

/**
 * [Kilde for denne](https://github.com/navikt/fp-abakus/blob/master/kontrakt-vedtak/src/main/java/no/nav/abakus/vedtak/ytelse/Ytelser.java#L3)
 * og [denne](https://github.com/navikt/fp-abakus/blob/master/kontrakt-vedtak/src/main/java/no/nav/abakus/vedtak/ytelse/v1/YtelseV1.java)
 *
 * @param ytelseStatus Mulige verdier: UNDER_BEHANDLING_LØPENDE,AVSLUTTET,UKJENT.
 * @param kildesystem Mulige verdier: FPSAK og K9SAK
 */
data class Ytelse(
    val ytelse: Ytelser,
    val saksnummer: String?,
    val kildesystem: String,
    val ytelseStatus: String,
    val vedtattTidspunkt: LocalDate,
    val anvist: List<Anvist>
)

data class Anvist(
    // Hva betyr denne? hva om det er en aktiv ytelse?
    // Kan slutt-dato *ikke* være satt?
    val periode: Periode,
    val utbetalingsgrad: Utbetalingsgrad,
    val beløp: Number?
)

data class Utbetalingsgrad(
    val verdi: Number
)

data class Aktør(
    val verdi: String
)

// Kopiert herfra: https://github.com/navikt/fp-abakus/blob/master/kontrakt-vedtak/src/main/java/no/nav/abakus/vedtak/ytelse/Ytelser.java#L3
enum class Ytelser {
    /**
     * Folketrygdloven K9 ytelser.
     */
    PLEIEPENGER_SYKT_BARN,
    PLEIEPENGER_NÆRSTÅENDE,
    OMSORGSPENGER,
    OPPLÆRINGSPENGER,

    /**
     * Folketrygdloven K14 ytelser.
     */
    ENGANGSTØNAD,
    FORELDREPENGER,
    SVANGERSKAPSPENGER
}
