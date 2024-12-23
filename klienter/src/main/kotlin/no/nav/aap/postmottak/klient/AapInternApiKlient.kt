package no.nav.aap.postmottak.klient

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.net.URI
import java.time.LocalDate

class AapInternApiKlient : AapInternApiGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.aap.intern.api.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.aap.intern.api.scope"),
    )
    private val client =
        RestClient.withDefaultResponseHandler(config = config, tokenProvider = ClientCredentialsTokenProvider)

    override fun hentArenaSakerForPerson(person: Person): List<String> {
        val path = url.resolve("/sakerByFnr")
        val reqbody =
            SakerRequest(personidentifikatorer = person.identer().map { it.identifikator })
        val saker: List<SakStatus> = client.post(path, PostRequest(body = reqbody), mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        })!!

        return saker.map { it.sakId }
    }
}

data class SakerRequest(
    val personidentifikatorer: List<String>
)

data class SakStatus(
    val sakId: String,
    val vedtakStatusKode: String,
    val periode: Periode
)

data class Periode(val fraOgMedDato: LocalDate?, val tilOgMedDato: LocalDate?)
