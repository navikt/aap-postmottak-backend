package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.httpklient.httpclient.tokenprovider.OidcToken
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.io.InputStream

interface DokumentGateway : Gateway {
    fun hentDokument(
        journalpostId: JournalpostId,
        dokumentId: DokumentInfoId,
        arkivtype: String = "ORIGINAL"
    ): SafDocumentResponse
}

interface DokumentOboGateway : Gateway {
    fun hentDokument(
        journalpostId: JournalpostId,
        dokumentId: DokumentInfoId,
        arkivtype: String = "ORIGINAL",
        currentToken: OidcToken,
    ): SafDocumentResponse
}

data class SafDocumentResponse(val dokument: InputStream, val contentType: String, val filnavn: String)
