package no.nav.aap.postmottak.klient.joark

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.saf.graphql.BrukerIdType
import no.nav.aap.postmottak.saf.graphql.SafGraphqlKlient
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.io.InputStream
import java.net.URI

interface Joark {
    fun førJournalpostPåFagsak(journalpostId: JournalpostId, ident: Ident, fagsakId: String, tema: String = "AAP")
    fun førJournalpostPåGenerellSak(journalpost: Journalpost, tema: String = "AAP")
    fun ferdigstillJournalpostMaskinelt(journalpostId: JournalpostId)
    fun ferdigstillJournalpost(journalpostId: JournalpostId, journalfoerendeEnhet: String)
}

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JoarkClient(
    private val client: RestClient<InputStream>,
    private val safGraphqlKlient: SafGraphqlKlient = SafGraphqlKlient.withClientCredentialsRestClient()
) : Joark {

    companion object {
        private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))
        val config = ClientConfig(
            scope = requiredConfigForKey("integrasjon.joark.scope"),
        )

        fun withClientCridentialsTokenProvider() =
            JoarkClient(
                RestClient.withDefaultResponseHandler(
                    config = config,
                    tokenProvider = ClientCredentialsTokenProvider
                ),
                SafGraphqlKlient.withClientCredentialsRestClient()
            )
    }

    override fun førJournalpostPåFagsak(journalpostId: JournalpostId, ident: Ident, fagsakId: String, tema: String) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpostId}")
        val request = PutRequest(
            OppdaterJournalpostRequest(
                journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
                sak = JournalpostSak(
                    fagsakId = fagsakId,
                ),
                tema = tema,
                bruker = JournalpostBruker(
                    id = ident.identifikator
                ),
                avsenderMottaker = hentAvsenderMottakerOmNødvendig(journalpostId)
            )
        )
        client.put(path, request) { _, _ -> }
    }

    override fun førJournalpostPåGenerellSak(journalpost: Journalpost, tema: String) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(
            OppdaterJournalpostRequest(
                journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
                sak = JournalpostSak(
                    sakstype = Sakstype.GENERELL_SAK,
                    fagsaksystem = null
                ),
                tema = tema,
                bruker = JournalpostBruker(
                    id = journalpost.person.aktivIdent().identifikator
                ),
                avsenderMottaker = hentAvsenderMottakerOmNødvendig(journalpost.journalpostId)
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
    }

    private fun hentAvsenderMottakerOmNødvendig(journalpostId: JournalpostId): AvsenderMottaker? {
        val safJournalpost = safGraphqlKlient.hentJournalpost(journalpostId)
        val avsenderMottaker = safJournalpost.avsenderMottaker
        val bruker = safJournalpost.bruker
        return if (avsenderMottaker == null || avsenderMottaker.id == null) {
            AvsenderMottaker(
                id = safJournalpost.bruker?.id!!,
                type = bruker?.type!!,
                erLikBruker = true
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
    val journalfoerendeEnhet: String,
    val sak: JournalpostSak,
    val tema: String,
    val bruker: JournalpostBruker,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avsenderMottaker: AvsenderMottaker?
)

data class AvsenderMottaker(
    val id: String,
    val type: BrukerIdType,
    val navn: String? = null,
    val land: String? = null,
    val erLikBruker: Boolean? = null,
)

enum class Fagsystem {
    KELVIN,
    AO01 // Arena
}

enum class Sakstype {
    FAGSAK,
    GENERELL_SAK
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
