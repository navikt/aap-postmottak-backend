package no.nav.aap.postmottak.klient.behandlingsflyt

import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.klient.oppgave.OppgaveKlient
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate

class BehandlingsflytClient : BehandlingsflytGateway {
    private val log = LoggerFactory.getLogger(SafGraphqlKlient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )


    companion object: Factory<BehandlingsflytClient> {
        override fun konstruer(): BehandlingsflytClient {
            return BehandlingsflytClient()
        }
    }

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

    override fun sendHendelse(
        journalpost: Journalpost,
        saksnummer: String,
        melding: Melding
    ) {
        // TODO bruk /api/hendelse/send i stedet
        val url = url.resolve("/api/hendelse/send")
        val request = PostRequest(
            Innsending(
                Saksnummer(saksnummer),
                InnsendingReferanse(
                    InnsendingReferanse.Type.JOURNALPOST,
                    journalpost.journalpostId.referanse.toString()
                ),
                InnsendingType.SØKNAD,
                journalpost.kanal.tilBehandlingsflytKanal(),
                journalpost.mottattDato.atStartOfDay(), //TODO: Avgjør hvilken dato vi skal bruke, og hvilket format
                melding
            ),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        client.post<Innsending, Unit>(url, request)
    }
}

fun KanalFraKodeverk.tilBehandlingsflytKanal(): Kanal {
    return when (this) {
        KanalFraKodeverk.SKAN_NETS -> Kanal.PAPIR
        KanalFraKodeverk.SKAN_PEN -> Kanal.PAPIR
        KanalFraKodeverk.SKAN_IM -> Kanal.PAPIR
        else -> Kanal.DIGITAL
    }
}
