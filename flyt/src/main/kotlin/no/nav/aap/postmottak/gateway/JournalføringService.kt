package no.nav.aap.postmottak.gateway

import com.fasterxml.jackson.annotation.JsonInclude
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.Header
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.put
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.JournalføringsType
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.journalføringCounter
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import java.io.InputStream
import java.net.URI

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JournalføringService(
    private val client: RestClient<InputStream>,
    private val safGraphqlKlient: JournalpostGateway,
    private val enhetsregisteretGateway: EnhetsregisteretGateway,
    val prometheus: MeterRegistry = SimpleMeterRegistry(),
    private val unleashGateway: UnleashGateway,
) {

    private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))

    companion object {
        fun konstruer(
            restClient: RestClient<InputStream>,
            safGraphqlKlient: JournalpostGateway,
            enhetsregisteretGateway: EnhetsregisteretGateway,
            prometheus: MeterRegistry = SimpleMeterRegistry(),
            unleashGateway: UnleashGateway,
        ): JournalføringService {
            return JournalføringService(
                restClient,
                safGraphqlKlient,
                enhetsregisteretGateway,
                prometheus,
                unleashGateway
            )
        }
    }

    constructor(gatewayProvider: GatewayProvider) : this(
        safGraphqlKlient = gatewayProvider.provide(JournalpostGateway::class),
        enhetsregisteretGateway = gatewayProvider.provide(EnhetsregisteretGateway::class),
        prometheus = PrometheusProvider.prometheus,
        client = RestClient.withDefaultResponseHandler(
            config = ClientConfig(
                scope = requiredConfigForKey("integrasjon.joark.scope"),
            ),
            tokenProvider = ClientCredentialsTokenProvider,
            prometheus = PrometheusProvider.prometheus,
        ),
        unleashGateway = gatewayProvider.provide<UnleashGateway>(),
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
        endretAv: Bruker?,
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")

        val request = PutRequest(
            body = OppdaterJournalpostRequest(
                sak = JournalpostSak(
                    fagsakId = fagsakId,
                    fagsaksystem = fagsystem,
                ),
                tema = tema,
                bruker = JournalpostBruker(
                    id = ident.identifikator
                ),
                tittel = tittel,
                avsenderMottaker = hentMottaker(avsenderMottaker, journalpostId),
                dokumenter = dokumenter
            ),
            additionalHeaders = navUserIdHeader(endretAv),
        )
        client.put<OppdaterJournalpostRequest, Unit>(path, request)
    }

    fun hentMottaker(avsenderMottaker: AvsenderMottakerDto?, journalpostId: JournalpostId): AvsenderMottakerDto? {
        val avsender = avsenderMottaker?.entenKunNavnEllerIdOgType() ?: hentAvsenderMottakerOmNødvendig(
            journalpostId
        )

        return if (avsender?.idType == AvsenderMottakerDto.IdType.ORGNR && avsender.id != null && unleashGateway.isEnabled(PostmottakFeature.EREGUtlandSjekk)) {
            val orgnr = Organisasjonsnummer(avsender.id)
            val eregRespons = enhetsregisteretGateway.hentOrganisasjon(orgnr)

            if (eregRespons?.fantIkke == true) {
                requireNotNull(avsenderMottaker?.navn) { "Navn på avsender må være satt for utenlands ORGNR" }
                AvsenderMottakerDto(
                    id = avsender.id,
                    idType = AvsenderMottakerDto.IdType.UTL_ORG,
                    navn = avsenderMottaker?.navn
                )
            } else {
                avsender
            }
        } else avsender
    }

    fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String = "AAP",
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null,
        endretAv: Bruker?,
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(
            body = OppdaterJournalpostRequest(
                sak = JournalpostSak(
                    sakstype = Sakstype.GENERELL_SAK,
                    fagsaksystem = null
                ),
                tema = tema,
                bruker = JournalpostBruker(
                    id = journalpost.person.aktivIdent().identifikator
                ),
                tittel = tittel,
                avsenderMottaker = avsenderMottaker?.entenKunNavnEllerIdOgType() ?: hentAvsenderMottakerOmNødvendig(
                    journalpost.journalpostId
                ),
                dokumenter = dokumenter
            ),
            additionalHeaders = navUserIdHeader(endretAv),
        )
        client.put(path, request) { _, _ -> }
    }

    fun ferdigstillJournalpostMaskinelt(
        journalpostId: JournalpostId,
        journalførtAv: Bruker?,
    ) {
        ferdigstillJournalpost(journalpostId, MASKINELL_JOURNALFØRING_ENHET, journalførtAv)
    }

    fun ferdigstillJournalpost(
        journalpostId: JournalpostId,
        journalfoerendeEnhet: String,
        journalførtAv: Bruker?,
    ) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/$journalpostId/ferdigstill")
        val request = PatchRequest(
            body = FerdigstillRequest(journalfoerendeEnhet),
            additionalHeaders = navUserIdHeader(journalførtAv),
        )
        client.patch(path, request) { _, _ -> }
        prometheus.journalføringCounter(type = JournalføringsType.automatisk).increment()
    }

    private fun navUserIdHeader(endretAv: Bruker?): List<Header> =
        listOfNotNull(
            endretAv?.let {
                Header("Nav-User-Id", it.ident)
            }
        )


    private fun hentAvsenderMottakerOmNødvendig(journalpostId: JournalpostId): AvsenderMottakerDto? {
        val safJournalpost = safGraphqlKlient.hentJournalpost(journalpostId)
        val avsenderMottaker = safJournalpost.avsenderMottaker
        val bruker = safJournalpost.bruker!!
        return if (avsenderMottaker?.id == null && bruker.type in listOf(BrukerIdType.FNR, BrukerIdType.ORGNR)) {
            AvsenderMottakerDto(
                id = safJournalpost.bruker.id,
                idType = AvsenderMottakerDto.IdType.valueOf(bruker.type?.name!!),
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
        val id: String?,
        val idType: IdType?,
        val navn: String? = null,
    ) {
        /**
         * Navn må være satt ELLER både id og idType må være satt
         * Helst bør alle tre være satt
         * Se https://confluence.adeo.no/spaces/BOA/pages/313346834/oppdaterJournalpost
         **/
        fun erGyldig(): Boolean = navn != null || (id != null && idType != null)

        /**
         * Dokarkiv aksepterer kun disse ved oppdatering/ferdigstilling av journalpost
         **/
        enum class IdType {
            FNR, ORGNR, HPRNR, UTL_ORG,
        }

        fun entenKunNavnEllerIdOgType(): AvsenderMottakerDto {
            return if (id != null && idType != null) {
                this.copy(navn = null)
            } else {
                this.copy(id = null, idType = null)
            }
        }
    }

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
