package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.flate

import no.nav.aap.postmottak.klient.joark.Dokument


data class DokumentInfoResponsDTO(val søker: DokumentIdent?, val dokumenter: List<DokumentDto>)
data class DokumentIdent(val ident: String, val navn: String)
data class DokumentDto(
    val dokumentInfoId: String,
) {
    companion object {
        fun fromDokument(dokument: Dokument) = DokumentDto(
            dokumentInfoId = dokument.dokumentInfoId.toString(),
        )
    }
}
