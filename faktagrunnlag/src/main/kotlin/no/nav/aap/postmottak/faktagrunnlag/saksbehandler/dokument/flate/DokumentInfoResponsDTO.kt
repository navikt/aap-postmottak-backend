package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.flate

import no.nav.aap.postmottak.sakogbehandling.behandling.Dokument


data class DokumentInfoResponsDTO(val søker: DokumentIdent?, val tittel: String, val dokumenter: List<DokumentDto>)
data class DokumentIdent(val ident: String, val navn: String)
data class DokumentDto(
    val dokumentInfoId: String,
    val tittel: String?
) {
    companion object {
        fun fromDokument(dokument: Dokument) = DokumentDto(
            dokumentInfoId = dokument.dokumentInfoId.toString(),
            tittel = dokument.tittel
        )
    }
}
