package no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import kotlinx.coroutines.runBlocking
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.net.URI
import java.time.LocalDate

interface BehandlingsflytGateway {
    fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): Saksinfo
    fun sendSøknad(sakId: String, journalpostId: JournalpostId, søknad: ByteArray)
}

class BehandlingsflytClient() : BehandlingsflytGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )


    override fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): Saksinfo {

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

    override fun sendSøknad(
        sakId: String,
        journalpostId: JournalpostId,
        søknad: ByteArray,
    ) {
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

private val objectMapper = ObjectMapper()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    .registerModule(JavaTimeModule())
    .registerModule(
        KotlinModule.Builder()
            .withReflectionCacheSize(512)
            .configure(KotlinFeature.NullToEmptyCollection, false)
            .configure(KotlinFeature.NullToEmptyMap, false)
            .configure(KotlinFeature.NullIsSameAsDefault, false)
            .configure(KotlinFeature.SingletonSupport, true)
            .configure(KotlinFeature.StrictNullChecks, false)
            .build()
    )
