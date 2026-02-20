package no.nav.aap.postmottak.klient

import no.nav.aap.api.intern.PersonEksistererIAAPArena
import no.nav.aap.api.intern.SignifikanteSakerRequest
import no.nav.aap.api.intern.SignifikanteSakerResponse
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.net.URI
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class AapInternApiKlient : AapInternApiGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.aap.intern.api.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.aap.intern.api.scope"),
        connectionTimeout = 2.minutes.toJavaDuration()
    )
    private val client =
        RestClient.withDefaultResponseHandler(
            config = config,
            tokenProvider = ClientCredentialsTokenProvider,
            prometheus = PrometheusProvider.prometheus
        )

    companion object : Factory<AapInternApiKlient> {
        override fun konstruer(): AapInternApiKlient {
            return AapInternApiKlient()
        }
    }

    /**
     * Arena-saker baserer seg på vedtak i Arena
     */
    override fun harAapSakIArena(person: Person): PersonEksistererIAAPArena {
        val path = url.resolve("/arena/person/aap/eksisterer")
        val reqbody = SakerRequest(personidentifikatorer = person.identer().map { it.identifikator })
        val response: PersonEksistererIAAPArena? = client.post(path, PostRequest(body = reqbody), mapper = { body, _ ->
            DefaultJsonMapper.fromJson<PersonEksistererIAAPArena>(body)
        })
        return requireNotNull(response) { "Kunne ikke sjekke om personen har vedtak i Arena" }
    }

    override fun harSignifikantHistorikkIAAPArena(
        person: Person,
        mottattDato: LocalDate
    ): SignifikanteSakerResponse {
        val path = url.resolve("/arena/person/aap/signifikant-historikk")
        val reqbody = SignifikanteSakerRequest(
            personidentifikatorer = person.identer().map { it.identifikator },
            virkningstidspunkt = mottattDato
        )
        val response: SignifikanteSakerResponse? = client.post(path, PostRequest(body = reqbody), mapper = { body, _ ->
            DefaultJsonMapper.fromJson<SignifikanteSakerResponse>(body)
        })
        requireNotNull(response) { "Kunne ikke sjekke om personen har signifikante saker i Arena" }
        return response
    }
}

data class SakerRequest(
    val personidentifikatorer: List<String>
)

data class SignifikanteSakerRequest(
    val personidentifikatorer: List<String>,
    val virkningstidspunkt: LocalDate
)
