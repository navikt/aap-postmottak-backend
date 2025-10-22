package no.nav.aap.postmottak.journalpostogbehandling.journalpost.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentDokumentDTO(
    @param:PathParam(description = "Journalpost-ID") val journalpostId: Long,
    @param:PathParam(description = "Dokumentinfo-ID") val dokumentinfoId: String
)
