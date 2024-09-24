package no.nav.aap.postmottak.joark

import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Ident
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.adapters.saf.Journalpost
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import java.net.URI

interface Joark {
    fun oppdaterJournalpost(journalpost: Journalpost.MedIdent, fagsakId: String)

    fun ferdigstillJournalpost(journalpost: Journalpost)
}

const val FORDELINGSOPPGAVE = "FDR"
const val JOURNALFORINGSOPPGAVE = "JFR"

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

    override fun oppdaterJournalpost(journalpost: Journalpost.MedIdent, fagsakId: String) {
        val ident = when (journalpost.personident) {
            is Ident.Personident -> journalpost.personident.id
            is Ident.Aktørid -> error("AktørID skal være byttet ut med folkeregisteridentifikator på dette tidspunktet")
        }

        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}")
        val request = PutRequest(OppdaterJournalpostRequest(
            journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET,
            sak = JournalpostSak(
                fagsakId = fagsakId
            ),
            bruker = JournalpostBruker(
                id = ident
            )
        ))
        client.put(path, request) { _,_ -> }

    }

    override fun ferdigstillJournalpost(journalpost: Journalpost) {
        val path = url.resolve("/rest/journalpostapi/v1/journalpost/${journalpost.journalpostId}/ferdigstill")
        val request = PatchRequest(FerdigstillRequest(journalfoerendeEnhet = MASKINELL_JOURNALFØRING_ENHET))
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

data class JournalpostSak(
    val sakstype: String = "FAGSAK",
    val fagsakId: String,
    val fagsaksystem: String = "KELVIN"
)

data class JournalpostBruker(
    val id: String,
    val idType: String = "FNR"
)
