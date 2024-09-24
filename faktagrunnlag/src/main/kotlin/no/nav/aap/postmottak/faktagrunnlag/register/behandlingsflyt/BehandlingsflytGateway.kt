package no.nav.aap.postmottak.faktagrunnlag.register.behandlingsflyt

import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksinfo
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClient
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

interface BehandlingsflytGateway {
    fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): Saksinfo
    fun finnSaker(ident: Ident): List<Saksinfo>
    fun sendSøknad(sakId: String, journalpostId: JournalpostId, søknad: ByteArray)
}

class BehandlingsflytClient() : BehandlingsflytGateway {
    private val log = LoggerFactory.getLogger(SafGraphqlClient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): Saksinfo {
        log.info("Finn eller opprett sak på person i behandlingsflyt")
        return runBlocking { finnEllerOpprett(ident.identifikator, mottattDato) }
    }

    private fun finnEllerOpprett(ident: String, mottattDato: LocalDate): Saksinfo {
        val request = PostRequest(
            FinnEllerOpprettSak(ident, mottattDato),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return client.post(url.resolve("/api/sak/finnEllerOpprett"), request)
            ?: throw UnknownError("Fikk uforventet respons fra behandlingsflyt")

    }
    
    override fun finnSaker(ident: Ident): List<Saksinfo> {
        log.info("Finn saker for person i behandlingsflyt")
        return runBlocking { finn(ident) }
    }
    
    private fun finn(ident: Ident): List<Saksinfo> {
        val request = PostRequest(
            FinnSaker(ident.identifikator)
        )
        return client.post(url.resolve("/api/sak/finn"), request)
            ?: throw UnknownError("Fikk uforventet respons fra behandlingsflyt")
    }

    override fun sendSøknad(
        sakId: String,
        journalpostId: JournalpostId,
        søknad: ByteArray,
    ) {
        val søknadStirng = String(søknad)
        val url = url.resolve("/api/soknad/send")
        val request = PostRequest(
            SendSøknad(sakId, journalpostId.toString(), søknadStirng),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        client.post<SendSøknad, Unit>(url, request)
    }
}
