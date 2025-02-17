package no.nav.aap.postmottak.klient

import no.nav.aap.api.intern.SakStatus
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.gateway.AapInternApiGateway
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import java.net.URI

class AapInternApiKlient : AapInternApiGateway {
    private val url = URI.create(requiredConfigForKey("integrasjon.aap.intern.api.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.aap.intern.api.scope"),
    )
    private val client =
        RestClient.withDefaultResponseHandler(config = config, tokenProvider = ClientCredentialsTokenProvider)
    
    companion object: Factory<AapInternApiKlient> {
        override fun konstruer(): AapInternApiKlient {
            return AapInternApiKlient()
        }
    }

    /**
     * Arena-saker baserer seg på vedtak i Arena
     */
    override fun hentAapSakerForPerson(person: Person): List<SakStatus> {
        val path = url.resolve("/sakerByFnr")
        val reqbody =
            SakerRequest(personidentifikatorer = person.identer().map { it.identifikator })
        val saker: List<SakStatus> = client.post(path, PostRequest(body = reqbody), mapper = { body, _ ->
            DefaultJsonMapper.fromJson(body)
        })!!

        return saker
    }
}

data class SakerRequest(
    val personidentifikatorer: List<String>
)