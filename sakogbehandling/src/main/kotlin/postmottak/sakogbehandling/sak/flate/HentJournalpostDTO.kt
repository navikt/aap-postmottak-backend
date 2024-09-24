package no.nav.aap.postmottak.sakogbehandling.sak.flate

import com.papsign.ktor.openapigen.annotations.parameters.PathParam

data class HentJournalpostDTO(@PathParam(description = "Journalpost-ID") val journalpostId: Long)