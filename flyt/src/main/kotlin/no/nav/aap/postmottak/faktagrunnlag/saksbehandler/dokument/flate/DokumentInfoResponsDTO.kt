package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.flate

import no.nav.aap.postmottak.gateway.SafDokumentInfo


data class DokumentInfoResponsDTO(val journalpostId: Long, val søker: DokumentIdent?, val avsender: DokumentIdent?, val dokumenter: List<DokumentDto>)
data class DokumentIdent(val ident: String?, val navn: String?)
data class DokumentDto(
    val dokumentInfoId: String,
    val tittel: String?
) {
    companion object {
        fun fromDokument(dokument: SafDokumentInfo) = DokumentDto(
            dokumentInfoId = dokument.dokumentInfoId,
            tittel = dokument.tittel
        )
    }
}
