package no.nav.aap.postmottak.klient.joark

import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.request.PatchRequest
import no.nav.aap.komponenter.httpklient.httpclient.request.PutRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto
import no.nav.aap.postmottak.gateway.FerdigstillRequest
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.JournalpostBruker
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.JournalpostSak
import no.nav.aap.postmottak.gateway.OppdaterJournalpostRequest
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.io.InputStream
import java.net.URI

private const val MASKINELL_JOURNALFØRING_ENHET = "9999"

class JoarkClient(private val client: RestClient<InputStream>, private val safGraphqlKlient: JournalpostGateway): JournalføringsGateway {

    private val url = URI.create(requiredConfigForKey("integrasjon.joark.url"))

    companion object: Factory<JoarkClient> {
        override fun konstruer(): JoarkClient {
            val restClient = RestClient.withDefaultResponseHandler(
                config = ClientConfig(
                    scope = requiredConfigForKey("integrasjon.joark.scope"),
                ),
                tokenProvider = ClientCredentialsTokenProvider
            )
            return JoarkClient(restClient, GatewayProvider.provide(JournalpostGateway::class))
        }
        fun konstruer(restClient: RestClient<InputStream>, safGraphqlKlient: JournalpostGateway): JoarkClient {
            return JoarkClient(restClient, safGraphqlKlient)
        }
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

    private fun hentAvsenderMottakerOmNødvendig(journalpostId: JournalpostId): AvsenderMottakerDto? {
        val safJournalpost = safGraphqlKlient.hentJournalpost(journalpostId)
        val avsenderMottaker = safJournalpost.avsenderMottaker
        val bruker = safJournalpost.bruker
        return if (avsenderMottaker?.id == null) {
            AvsenderMottakerDto(
                id = safJournalpost.bruker?.id!!,
                type = bruker?.type!!,
                erLikBruker = true
            )
        } else {
            null
        }
    }
}

