package mottak.saf

import SafResponseHandler
import kotlinx.coroutines.runBlocking
import no.nav.aap.behandlingsflyt.saf.Dokument
import no.nav.aap.behandlingsflyt.saf.Filtype
import no.nav.aap.behandlingsflyt.saf.Ident
import no.nav.aap.behandlingsflyt.saf.Journalpost
import no.nav.aap.behandlingsflyt.saf.JournalpostStatus
import no.nav.aap.behandlingsflyt.saf.Variantformat
import no.nav.aap.behandlingsflyt.saf.graphql.BrukerIdType
import no.nav.aap.behandlingsflyt.saf.graphql.Journalstatus
import no.nav.aap.behandlingsflyt.saf.graphql.SafDatoType
import no.nav.aap.behandlingsflyt.saf.graphql.SafJournalpost
import no.nav.aap.behandlingsflyt.saf.graphql.SafRequest
import no.nav.aap.behandlingsflyt.saf.graphql.SafRespons
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.komponenter.config.requiredConfigForKey
import no.nav.aap.komponenter.httpklient.httpclient.ClientConfig
import no.nav.aap.komponenter.httpklient.httpclient.RestClient
import no.nav.aap.komponenter.httpklient.httpclient.post
import no.nav.aap.komponenter.httpklient.httpclient.request.PostRequest
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.azurecc.ClientCredentialsTokenProvider
import org.slf4j.LoggerFactory
import java.net.URI

interface SafGraphqlGateway {
    fun hentJournalpost(journalpostId: JournalpostId): Journalpost
}

object SafGraphqlClient : SafGraphqlGateway {
    private val log = LoggerFactory.getLogger(SafGraphqlClient::class.java)

    val config = ClientConfig(
        scope = requiredConfigForKey("integrasjon.saf.scope"),
    )

    private val graphqlUrl = URI.create(requiredConfigForKey("integrasjon.saf.url.graphql"))

    private val client = RestClient(
        config = config,
        tokenProvider = ClientCredentialsTokenProvider,
        responseHandler = SafResponseHandler()
    )

    override fun hentJournalpost(journalpostId: JournalpostId): Journalpost {
        val request = SafRequest.hentJournalpost(journalpostId)
        val response = runBlocking { graphqlQuery(request) }

        val journalpost: SafJournalpost = response.data?.journalpost
            ?: error("Fant ikke journalpost for $journalpostId")

        val ident = when (journalpost.bruker?.type) {
            BrukerIdType.AKTOERID -> null //Ident.Aktørid(journalpost.bruker.id!!) //TODO: Må håndtere aktørid bittelitt mer fornuftig
            BrukerIdType.FNR -> Ident.Personident(journalpost.bruker.id!!)
            else -> null.also {
                log.warn("mottok noe annet enn personnr: ${journalpost.bruker?.type}")
            }
        }

        val mottattDato = journalpost.relevanteDatoer?.find { dato ->
            dato?.datotype == SafDatoType.DATO_REGISTRERT
        }?.dato?.toLocalDate() ?: error("Fant ikke dato")

        val dokumenter = journalpost.dokumenter?.filterNotNull()?.flatMap { dokument ->
            dokument.dokumentvarianter.filterNotNull().map { variant ->
                Dokument(
                    dokument.dokumentInfoId,
                    Variantformat.valueOf(variant.variantformat.name),
                    Filtype.valueOf(variant.filtype),
                    dokument.brevkode
                )
            }
        } ?: emptyList()

        return if (ident == null) {
            Journalpost.UtenIdent(
                journalpostId = journalpost.journalpostId,
                status = finnJournalpostStatus(journalpost.journalstatus),
                journalførendeEnhet = journalpost.journalfoerendeEnhet,
                mottattDato = mottattDato,
                dokumenter = dokumenter
            )
        } else {
            Journalpost.MedIdent(
                personident = ident,
                journalpostId = journalpost.journalpostId,
                status = finnJournalpostStatus(journalpost.journalstatus),
                journalførendeEnhet = journalpost.journalfoerendeEnhet,
                mottattDato = mottattDato,
                dokumenter = dokumenter
            )
        }
    }

    private fun graphqlQuery(query: SafRequest): SafRespons {
        val request = PostRequest(query)
        return requireNotNull(client.post(uri = graphqlUrl, request))
    }

    private fun finnJournalpostStatus(status: Journalstatus?): JournalpostStatus {
        return when (status) {
            Journalstatus.MOTTATT -> JournalpostStatus.MOTTATT
            else -> JournalpostStatus.UKJENT
        }
    }
}