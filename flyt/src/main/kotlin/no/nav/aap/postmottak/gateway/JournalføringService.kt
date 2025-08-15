package no.nav.aap.postmottak.gateway

import com.fasterxml.jackson.annotation.JsonInclude
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.JournalføringsType
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.journalføringCounter
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.io.InputStream
import java.net.URI

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JournalføringService(
    private val client: RestClient<InputStream>,
    private val safGraphqlKlient: JournalpostGateway,
    private val persondataGateway: PersondataGateway,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
) {

    private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))

    companion object {
        fun konstruer(
            restClient: RestClient<InputStream>,
            safGraphqlKlient: JournalpostGateway,
            persondataGateway: PersondataGateway,
            prometheus: MeterRegistry = SimpleMeterRegistry()
        ): JournalføringService {
            return JournalføringService(restClient, safGraphqlKlient, persondataGateway, prometheus)
        }
    }

    constructor(gatewayProvider: GatewayProvider) : this(
        safGraphqlKlient = gatewayProvider.provide(JournalpostGateway::class),
        persondataGateway = gatewayProvider.provide(PersondataGateway::class),
        prometheus = PrometheusProvider.prometheus,
        client = RestClient.withDefaultResponseHandler(
            config = ClientConfig(
                scope = requiredConfigForKey("integrasjon.joark.scope"),
            ),
            tokenProvider = ClientCredentialsTokenProvider,
            prometheus = PrometheusProvider.prometheus
        )
    )

     fun førJournalpostPåFagsak(
        journalpostId: JournalpostId,
        ident: Ident,
        fagsakId: String,
        tema: String = "AAP",
        fagsystem: Fagsystem = Fagsystem.KELVIN,
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null,
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

     fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String = "AAP",
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null
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

     fun ferdigstillJournalpostMaskinelt(journalpostId: JournalpostId) {
        ferdigstillJournalpost(journalpostId, MASKINELL_JOURNALFØRING_ENHET)
    }

     fun ferdigstillJournalpost(journalpostId: JournalpostId, journalfoerendeEnhet: String) {
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

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)

data class OppdaterJournalpostRequest(
    val behandlingstema: String? = null,
    val sak: JournalpostSak,
    val tema: String,
    val bruker: JournalpostBruker,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val tittel: String?,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val avsenderMottaker: AvsenderMottakerDto?,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val dokumenter: List<ForenkletDokument>?
)

data class AvsenderMottakerDto(
    val id: String,
    val idType: BrukerIdType,
    val navn: String? = null,
)

enum class Fagsystem {
    KELVIN,
    AO01, // Arena
    FS22 // Generell sak
}

data class JournalpostSak(
    val sakstype: Sakstype = Sakstype.FAGSAK,
    val fagsakId: String? = null,
    val fagsaksystem: Fagsystem? = Fagsystem.KELVIN
)

data class JournalpostBruker(
    val id: String,
    val idType: String = "FNR"
)
