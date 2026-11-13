package no.nav.aap.postmottak.klient.dokarkiv

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.AzureM2MTokenProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.PrometheusProvider.Companion.prometheus
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.gateway.DokarkivGateway
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.JournalpostSak
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Behandlingstema
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import java.util.*

class DokarkivGatewayKlient(
    private val restClient: RestClient<InputStream> = client,
) : DokarkivGateway {
    private var log = LoggerFactory.getLogger(javaClass)

    companion object : Factory<DokarkivGateway> {
        private val url = URI.create(requiredConfigForKey("INTEGRASJON_JOARK_URL"))
        private val client: RestClient<InputStream> by lazy {
            RestClient.withDefaultResponseHandler(
                config = ClientConfig(
                    scope = requiredConfigForKey("INTEGRASJON_JOARK_SCOPE"),
                ),
                tokenProvider = AzureM2MTokenProvider,
                prometheus = prometheus,
            )
        }

        override fun konstruer(): DokarkivGateway {
            return DokarkivGatewayKlient()
        }
    }

    override fun endreTema(
        journalpostId: JournalpostId, tema: String, endretAv: Bruker?
    ) {
        restClient.put<EndreTemaRequest, Unit>(
            url.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId"),
            PutRequest(EndreTemaRequest(tema), additionalHeaders = navUserIdHeader(endretAv))
        )
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
        endretAv: Bruker?
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")

        val request = PutRequest(
            body = OppdaterJournalpostRequest(
                sak = JournalpostSak(
                    fagsakId = fagsakId,
                    fagsaksystem = fagsystem,
                ), tema = tema, bruker = JournalpostBruker(
                    id = ident.identifikator
                ), tittel = tittel, avsenderMottaker = avsenderMottaker, dokumenter = dokumenter
            ),
            additionalHeaders = navUserIdHeader(endretAv),
        )
        restClient.put<OppdaterJournalpostRequest, Unit>(path, request)
    }

    override fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String,
        tittel: String?,
        avsenderMottaker: AvsenderMottakerDto?,
        dokumenter: List<ForenkletDokument>?,
        endretAv: Bruker?
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(
            body = OppdaterJournalpostRequest(
                sak = JournalpostSak(
                    sakstype = Sakstype.GENERELL_SAK, fagsaksystem = null
                ), tema = tema, bruker = JournalpostBruker(
                    id = journalpost.person.aktivIdent().identifikator
                ), tittel = tittel, avsenderMottaker = avsenderMottaker, dokumenter = dokumenter
            ),
            additionalHeaders = navUserIdHeader(endretAv),
        )
        restClient.put(path, request) { _, _ -> }
    }

    override fun kopierJournalpost(journalpost: Journalpost): JournalpostId {
        val eksternReferanseId = UUID.randomUUID().toString()
        log.info("Kopierer Journalpost ${journalpost.journalpostId} med eksternReferanseId=$eksternReferanseId")
        val path =
            url.resolve("/rest/journalpostapi/v1/journalpost/kopierJournalpost?kildeJournalpostId=${journalpost.journalpostId}")
        return restClient.post<KopierJournalpostRequest, KopierJournalpostResponse>(
            path, PostRequest(KopierJournalpostRequest(eksternReferanseId = eksternReferanseId))
        )?.let { JournalpostId(it.kopierJournalpostId.toLong()) }
            ?: error("Kunne ikke kopiere journalpost med id ${journalpost.journalpostId}")
    }

    override fun opprettJournalpostForPerson(
        ident: Ident, pdf: ByteArray, filNavn: String, eksternReferanseId: UUID?
    ): JournalpostId {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost?forsoekFerdigstilling=false")
        val request = OpprettJournalpostRequest(
            journalposttype = OpprettJournalpostType.INNGAAENDE.name,
            tema = Tema.AAP.toString(),
            behandlingstema = Behandlingstema.AAP.kode,
            kanal = KanalFraKodeverk.INNSENDT_NAV_ANSATT.toString(),
            eksternReferanseId = eksternReferanseId?.toString(),
            tittel = "Ettersendelse for arbeidsavklaringspenger sendt inn av saksbehandler på vegne av innbygger",
            bruker = JournalpostBruker(id = ident.identifikator),
            avsenderMottaker = AvsenderMottakerDto(id = ident.identifikator, idType = AvsenderMottakerDto.IdType.FNR),
            dokumenter = listOf(
                NyttDokumentRequest(
                    tittel = filNavn, brevkode = Brevkoder.STANDARD_ETTERSENDING.kode, dokumentvarianter = listOf(
                        NyttDokumentvariantRequest(
                            filtype = "PDF",
                            variantformat = "ARKIV",
                            fysiskDokument = Base64.getEncoder().encodeToString(pdf)
                        )
                    )
                )
            )
        )
        val response = requireNotNull(
            restClient.post<OpprettJournalpostRequest, OpprettJournalpostResponse>(
                path, PostRequest(request)
            )
        ) { "Kunne ikke opprette journalpost for person" }
        return JournalpostId(response.journalpostId.toLong())
    }

    override fun ferdigstillJournalpost(
        journalpostId: JournalpostId, journalfoerendeEnhet: String, journalførtAv: Bruker?
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill")
        val request = PatchRequest(
            body = FerdigstillRequest(journalfoerendeEnhet),
            additionalHeaders = navUserIdHeader(journalførtAv),
        )
        restClient.patch(path, request) { _, _ -> }
    }

    private fun navUserIdHeader(endretAv: Bruker?): List<Header> = listOfNotNull(
        endretAv?.let {
            Header("Nav-User-Id", it.ident)
        })
}

data class EndreTemaRequest(val tema: String)

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)


data class KopierJournalpostRequest(
    val eksternReferanseId: String
)

data class KopierJournalpostResponse(
    val kopierJournalpostId: String
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

data class JournalpostBruker(
    val id: String,
    val idType: String = "FNR"
)

data class OpprettJournalpostResponse(
    val journalpostId: String,
)


data class OpprettJournalpostRequest(
    val journalposttype: String,
    val tema: String,
    val behandlingstema: String,
    val kanal: String,
    val eksternReferanseId: String?,
    val tittel: String,
    val bruker: JournalpostBruker,
    val avsenderMottaker: AvsenderMottakerDto,
    val dokumenter: List<NyttDokumentRequest>,
)

data class NyttDokumentRequest(
    val tittel: String,
    val brevkode: String,
    val dokumentvarianter: List<NyttDokumentvariantRequest>,
)

data class NyttDokumentvariantRequest(
    val filtype: String,
    val variantformat: String,
    val fysiskDokument: String,
)


enum class OpprettJournalpostType {
    INNGAAENDE, UTGAAENDE, NOTAT
}