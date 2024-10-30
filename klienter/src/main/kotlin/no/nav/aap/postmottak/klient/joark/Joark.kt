package no.nav.aap.postmottak.klient.joark

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import java.net.URI

interface Joark {
    fun førJournalpostPåFagsak(journalpost: Journalpost, fagsakId: String)
    fun førJournalpostPåGenerellSak(journalpost: Journalpost)
    fun ferdigstillJournalpostMaskinelt(journalpost: Journalpost)
    fun ferdigstillJournalpost(journalpost: Journalpost, journalfoerendeEnhet: String)
}

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JoarkClient: Joark {

    private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))
    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.joark.scope"),
    )
    private val client = RestClient.withDefaultResponseHandler(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
    )

    override fun førJournalpostPåFagsak(journalpost: Journalpost, fagsakId: String) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(
            OppdaterJournalpostRequest(
            journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
            sak = JournalpostSak(
                fagsakId = fagsakId,
            ),
            bruker = JournalpostBruker(
                id = journalpost.person.aktivIdent().identifikator
            )
        )
        )
        client.put(path, request) { _,_ -> }
    }

    override fun førJournalpostPåGenerellSak(journalpost: Journalpost) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(
            OppdaterJournalpostRequest(
            journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
            sak = JournalpostSak(
                sakstype = Sakstype.GENERELL_SAK,
                fagsaksystem = null
            ),
            bruker = JournalpostBruker(
                id = journalpost.person.aktivIdent().identifikator
            )
        )
        )
        client.put(path, request) { _,_ -> }
    }

    override fun ferdigstillJournalpostMaskinelt(journalpost: Journalpost) {
        ferdigstillJournalpost(journalpost, MASKINELL_JOURNALFØRING_ENHET)
    }

    override fun ferdigstillJournalpost(journalpost: Journalpost, journalfoerendeEnhet: String) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}/ferdigstill")
        val request = PatchRequest(FerdigstillRequest(journalfoerendeEnhet))
        client.patch(path, request) { _,_ -> }
    }
}

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)

data class OppdaterJournalpostRequest(
    val behandlingstema: String? = null,
    val journalfoerendeEnhet: String,
    val sak: JournalpostSak,
    val tema: String = "AAP",
    val bruker: JournalpostBruker
)

enum class  Fagsystem {
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
