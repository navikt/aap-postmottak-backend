package no.nav.aap.postmottak.klient.behandlingsflyt

import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

interface BehandlingsflytKlient {
    fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): BehandlingsflytSak
    fun finnSaker(ident: Ident): List<BehandlingsflytSak>
    fun sendSøknad(sakId: String, journalpostId: JournalpostId, søknad: Søknad)
}

class BehandlingsflytClient : BehandlingsflytKlient {
    private val log = LoggerFactory.getLogger(SafGraphqlKlient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): BehandlingsflytSak {
        log.info("Finn eller opprett sak på person i behandlingsflyt")
        return runBlocking { finnEllerOpprett(ident.identifikator, mottattDato) }
    }

    private fun finnEllerOpprett(ident: String, mottattDato: LocalDate): BehandlingsflytSak {
        val request = PostRequest(
            FinnEllerOpprettSak(ident, mottattDato),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return client.post(url.resolve("/api/sak/finnEllerOpprett"), request)
            ?: throw UnknownError("Fikk uforventet respons fra behandlingsflyt")

    }
    
    override fun finnSaker(ident: Ident): List<BehandlingsflytSak> {
        log.info("Finn saker for person i behandlingsflyt")
        return runBlocking { finn(ident) }
    }
    
    private fun finn(ident: Ident): List<BehandlingsflytSak> {
        val request = PostRequest(
            FinnSaker(ident.identifikator)
        )
        return client.post(url.resolve("/api/sak/finn"), request)
            ?: throw UnknownError("Fikk uforventet respons fra behandlingsflyt")
    }

    override fun sendSøknad(
        sakId: String,
        journalpostId: JournalpostId,
        søknad: Søknad,
    ) {
        // TODO bruk /api/hendelse/send i stedet
        val url = url.resolve("/api/soknad/send")
        val request = PostRequest(
            SendSøknad(sakId, journalpostId.toString(), søknad),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        client.post<SendSøknad, Unit>(url, request)
    }
}

data class BehandlingsflytSak(
    val saksnummer: String,
    val periode: Periode,
)
