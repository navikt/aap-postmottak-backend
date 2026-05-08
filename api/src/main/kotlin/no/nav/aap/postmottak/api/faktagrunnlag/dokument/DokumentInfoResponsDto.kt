package no.nav.aap.postmottak.api.faktagrunnlag.dokument

import no.nav.aap.postmottak.gateway.SafDokumentInfo
import java.time.LocalDate


data class DokumentInfoResponsDTO(val journalpostId: Long, val søker: DokumentIdent?, val avsender: DokumentIdent?, val dokumenter: List<DokumentDto>, val registrertDato: LocalDate?)
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
