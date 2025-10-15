package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.gateway.AvsenderMottakerDto

data class Saksvurdering(
    val saksnummer: String? = null,
    val generellSak: Boolean = false,
    val opprettetNy: Boolean = false,
    val journalposttittel: String? = null,
    val avsenderMottaker: AvsenderMottakerDto? = null,
    val dokumenter: List<ForenkletDokument>? = null,
) {
    init {
        require(!saksnummer.isNullOrEmpty() || generellSak) { "Sak må oppgis" }
        require(!(opprettetNy && generellSak)) { "Opprettet ny må være fagsak" }

        if (journalposttittel != null) {
            require(journalposttittel.isNotBlank()) { "Journalposttittel kan ikke være tom" }
        }
        if (avsenderMottaker != null) {
            require(avsenderMottaker.erGyldig()) { "AvsenderMottaker er ikke gyldig" }
        }
        if (dokumenter != null) {
            require(dokumenter.isNotEmpty()) { "Dokumenter kan ikke være tom" }
        }

    }
}