package no.nav.aap.postmottak.klient.behandlingsflyt

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingReferanse
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Innsending
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Klagebehandling
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.dokument.Kanal
import org.slf4j.LoggerFactory
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.aap.behandlingsflyt.kontrakt.behandling.TypeBehandling as BehandlingsflytTypeBehandling

class BehandlingsflytKlient : BehandlingsflytGateway {
    private val log = LoggerFactory.getLogger(BehandlingsflytKlient::class.java)

    private val url = URI.create(requiredConfigForKey("integrasjon.behandlingsflyt.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.behandlingsflyt.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        prometheus = PrometheusProvider.prometheus
    )


    companion object : Factory<BehandlingsflytKlient> {
        override fun konstruer(): BehandlingsflytKlient {
            return BehandlingsflytKlient()
        }
    }

    override fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): BehandlingsflytSak {
        log.info("Finn eller opprett sak på person i behandlingsflyt")
        return finnEllerOpprett(ident.identifikator, mottattDato)
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
        return finn(ident)
    }

    private fun finn(ident: Ident): List<BehandlingsflytSak> {
        val request = PostRequest(
            FinnSaker(ident.identifikator)
        )
        // TODO: resultatet i ResultatKode utledes kun basert på førstegangsbehandling, som ikke nødvendigvis er nåværende tilstand for saken.
        return client.post(url.resolve("/api/sak/ekstern/finn"), request)
            ?: throw UnknownError("Fikk uforventet respons fra behandlingsflyt")
    }

    override fun sendHendelse(
        journalpostId: JournalpostId,
        kanal: KanalFraKodeverk,
        mottattDato: LocalDateTime,
        innsendingstype: InnsendingType,
        saksnummer: String,
        melding: Melding?
    ) {
        val url = url.resolve("/api/hendelse/send")
        val request = PostRequest(
            Innsending(
                Saksnummer(saksnummer),
                InnsendingReferanse(
                    InnsendingReferanse.Type.JOURNALPOST,
                    journalpostId.referanse.toString()
                ),
                innsendingstype,
                kanal.tilBehandlingsflytKanal(),
                mottattDato,
                melding
            ),
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        client.post<Innsending, Unit>(url, request)
    }

    override fun finnKlagebehandlinger(saksnummer: Saksnummer): List<Klagebehandling> {
        val url = url.resolve("/api/sak/${saksnummer}/finnBehandlingerAvType")
        val request = PostRequest(
            body = BehandlingsflytTypeBehandling.Klage,
            additionalHeaders = listOf(
                Header("Accept", "application/json")
            )
        )
        return client.post<BehandlingsflytTypeBehandling, List<Klagebehandling>>(url, request) ?: emptyList()
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