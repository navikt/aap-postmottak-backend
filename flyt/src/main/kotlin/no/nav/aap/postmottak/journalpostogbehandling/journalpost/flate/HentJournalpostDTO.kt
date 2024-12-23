package no.nav.aap.postmottak.journalpostogbehandling.journalpost.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentJournalpostDTO(@PathParam(description = "Journalpost-ID") val journalpostId: Long)