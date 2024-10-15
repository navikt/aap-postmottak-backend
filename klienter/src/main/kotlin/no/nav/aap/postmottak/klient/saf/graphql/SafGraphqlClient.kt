package no.nav.aap.postmottak.saf.graphql

import SafResponseHandler
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.OnBehalfOfTokenProvider
import no.nav.aap.postmottak.klient.joark.Dokument
import no.nav.aap.postmottak.klient.joark.Filtype
import no.nav.aap.postmottak.klient.joark.Ident
import no.nav.aap.postmottak.klient.joark.Journalpost
import no.nav.aap.postmottak.klient.joark.JournalpostStatus
import no.nav.aap.postmottak.klient.joark.DokumentInfoId
import no.nav.aap.postmottak.klient.joark.Variantformat
import org.slf4j.LoggerFactory
import java.io.InputStream
import java.net.URI
import kotlinx.coroutines.runBlocking


interface SafGraphqlGateway {
    fun hentJournalpost(journalpostId: JournalpostId, currentToken: OidcToken? = null): SafJournalpost
}

class SafGraphqlClient(private val restClient: RestClient<InputStream>) : SafGraphqlGateway {
    private val log = LoggerFactory.getLogger(SafGraphqlClient::class.java)

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    companion object {
        private fun getClientConfig() = ClientConfig(
            scope = requiredConfigForKey("integrasjon.saf.scope"),
        )

        fun withClientCredentialsRestClient() =
            SafGraphqlClient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = ClientCredentialsTokenProvider,
                    responseHandler = SafResponseHandler()
                )
            )

        fun withOboRestClient() =
            SafGraphqlClient(
                RestClient(
                    config = getClientConfig(),
                    tokenProvider = OnBehalfOfTokenProvider,
                    responseHandler = SafResponseHandler()
                )
            )
    }

    override fun hentJournalpost(journalpostId: JournalpostId, currentToken: OidcToken?): SafJournalpost {
        log.info("Henter journalpost: $journalpostId")
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = runBlocking { graphqlQuery(request, currentToken) }

        val journalpost: SafJournalpost = response.data?.journalpost
            ?: error("Fant ikke journalpost for $journalpostId")
        
        if (!listOf(BrukerIdType.FNR, BrukerIdType.AKTOERID).contains(journalpost.bruker?.type)) {
            log.warn("mottok noe annet enn aktør-id eller fnr: ${journalpost.bruker?.type}")
        }

        return journalpost
    }

    private fun graphqlQuery(query: SafRequest, currentToken: OidcToken?): SafRespons {
        val request = PostRequest(query, currentToken = currentToken)
        return requireNotNull(restClient.post(uri = graphqlUrl, request))
    }
}

fun SafJournalpost.tilJournalpost(): Journalpost {
    val journalpost = this
    val ident = when (journalpost.bruker?.type) {
        BrukerIdType.AKTOERID -> Ident.Aktørid(journalpost.bruker.id!!)
        BrukerIdType.FNR -> Ident.Personident(journalpost.bruker.id!!)
        else -> null
    }

    fun finnJournalpostStatus(status: Journalstatus?): JournalpostStatus {
        return when (status) {
            Journalstatus.MOTTATT -> JournalpostStatus.MOTTATT
            else -> JournalpostStatus.UKJENT
        }
    }

    val mottattDato = journalpost.relevanteDatoer?.find { dato ->
        dato?.datotype == SafDatoType.DATO_REGISTRERT
    }?.dato?.toLocalDate() ?: error("Fant ikke dato")

    val dokumenter = journalpost.dokumenter?.filterNotNull()?.flatMap { dokument ->
        dokument.dokumentvarianter.filterNotNull().map { variant ->
            Dokument(
                dokument.dokumentInfoId.let(::DokumentInfoId),
                Variantformat.valueOf(variant.variantformat.name),
                Filtype.valueOf(variant.filtype),
                dokument.brevkode,
            )
        }
    } ?: emptyList()

    return if (ident == null) {
        Journalpost.UtenIdent(
            journalpostId = journalpost.journalpostId.let(::JournalpostId),
            status = finnJournalpostStatus(journalpost.journalstatus),
            journalførendeEnhet = journalpost.journalfoerendeEnhet,
            mottattDato = mottattDato,
            dokumenter = dokumenter
        )
    } else {
        Journalpost.MedIdent(
            personident = ident,
            journalpostId = journalpost.journalpostId.let(::JournalpostId),
            status = finnJournalpostStatus(journalpost.journalstatus),
            journalførendeEnhet = journalpost.journalfoerendeEnhet,
            mottattDato = mottattDato,
            dokumenter = dokumenter
        )
    }
}