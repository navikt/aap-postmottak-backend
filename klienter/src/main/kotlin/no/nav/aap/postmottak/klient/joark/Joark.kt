package no.nav.aap.postmottak.klient.joark

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.JournalføringsType
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.FerdigstillRequest
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.JournalpostBruker
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostSak
import no.nav.aap.postmottak.gateway.OppdaterJournalpostRequest
import no.nav.aap.postmottak.gateway.PersondataGateway
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalføringCounter
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.io.InputStream
import java.net.URI

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JoarkClient(
    private val client: RestClient<InputStream>,
    private val safGraphqlKlient: JournalpostGateway,
    private val persondataGateway: PersondataGateway,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) : JournalføringsGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))

    companion object : Factory<JoarkClient> {
        override fun konstruer(): JoarkClient {
            val restClient = RestClient.withDefaultResponseHandler(
                config = ClientConfig(
                    scope = requiredConfigForKey("integrasjon.joark.scope"),
                ),
                tokenProvider = ClientCredentialsTokenProvider,
                prometheus = PrometheusProvider.prometheus,
            )
            return JoarkClient(
                restClient,
                GatewayProvider.provide(JournalpostGateway::class),
                GatewayProvider.provide(PersondataGateway::class),
                PrometheusProvider.prometheus
            )
        }

        fun konstruer(
            restClient: RestClient<InputStream>,
            safGraphqlKlient: JournalpostGateway,
            persondataGateway: PersondataGateway,
            prometheus: MeterRegistry = SimpleMeterRegistry()
        ): JoarkClient {
            return JoarkClient(restClient, safGraphqlKlient, persondataGateway, prometheus)
        }
    }

    override fun førJournalpostPåFagsak(
        journalpostId: JournalpostId,
        ident: Ident,
        fagsakId: String,
        tema: String,
        fagsystem: Fagsystem,
        tittel: String?,
        avsenderMottaker: AvsenderMottakerDto?,
        dokumenter: List<ForenkletDokument>?,
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")
        val request = PutRequest(
            OppdaterJournalpostRequest(
                sak = JournalpostSak(
                    fagsakId = fagsakId,
                    fagsaksystem = fagsystem,
                ),
                tema = tema,
                bruker = JournalpostBruker(
                    id = ident.identifikator
                ),
                tittel = tittel,
                avsenderMottaker = avsenderMottaker ?: hentAvsenderMottakerOmNødvendig(journalpostId),
                dokumenter = dokumenter
            )
        )
        client.put(path, request) { _, _ -> }
    }

    override fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String,
        tittel: String?,
        avsenderMottaker: AvsenderMottakerDto?,
        dokumenter: List<ForenkletDokument>?
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(
            OppdaterJournalpostRequest(
                sak = JournalpostSak(
                    sakstype = Sakstype.GENERELL_SAK,
                    fagsaksystem = null
                ),
                tema = tema,
                bruker = JournalpostBruker(
                    id = journalpost.person.aktivIdent().identifikator
                ),
                tittel = tittel,
                avsenderMottaker = avsenderMottaker ?: hentAvsenderMottakerOmNødvendig(journalpost.journalpostId),
                dokumenter = dokumenter
            )
        )
        client.put(path, request) { _, _ -> }
    }

    override fun ferdigstillJournalpostMaskinelt(journalpostId: JournalpostId) {
        ferdigstillJournalpost(journalpostId, MASKINELL_JOURNALFØRING_ENHET)
    }

    override fun ferdigstillJournalpost(journalpostId: JournalpostId, journalfoerendeEnhet: String) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill")
        val request = PatchRequest(FerdigstillRequest(journalfoerendeEnhet))
        client.patch(path, request) { _, _ -> }
        prometheus.journalføringCounter(type = JournalføringsType.automatisk).increment()
    }

    private fun hentAvsenderMottakerOmNødvendig(journalpostId: JournalpostId): AvsenderMottakerDto? {
        val safJournalpost = safGraphqlKlient.hentJournalpost(journalpostId)
        val avsenderMottaker = safJournalpost.avsenderMottaker
        val bruker = safJournalpost.bruker
        val navn = persondataGateway.hentNavn(bruker?.id!!)
        return if (avsenderMottaker?.id == null) {
            AvsenderMottakerDto(
                id = safJournalpost.bruker?.id!!,
                idType = bruker.type!!,
                navn = navn?.fulltNavn(),
            )
        } else {
            null
        }
    }
}

