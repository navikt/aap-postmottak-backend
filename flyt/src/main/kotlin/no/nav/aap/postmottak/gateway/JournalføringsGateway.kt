package no.nav.aap.postmottak.gateway

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface JournalføringsGateway: Gateway {
    fun førJournalpostPåFagsak(journalpostId: JournalpostId, ident: Ident, fagsakId: String, tema: String = "AAP", fagsystem: Fagsystem = Fagsystem.KELVIN)
    fun førJournalpostPåGenerellSak(journalpost: Journalpost, tema: String = "AAP")
    fun ferdigstillJournalpostMaskinelt(journalpostId: JournalpostId)
    fun ferdigstillJournalpost(journalpostId: JournalpostId, journalfoerendeEnhet: String)
}

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)

data class OppdaterJournalpostRequest(
    val behandlingstema: String? = null,
    val journalfoerendeEnhet: String,
    val sak: JournalpostSak,
    val tema: String,
    val bruker: JournalpostBruker,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val avsenderMottaker: AvsenderMottakerDto?
)

data class AvsenderMottakerDto(
    val id: String,
    val type: BrukerIdType,
    val navn: String? = null,
    val land: String? = null,
    val erLikBruker: Boolean? = null,
)

enum class Fagsystem {
    KELVIN,
    AO01 // Arena
}

data class JournalpostSak(
    val sakstype: Sakstype = Sakstype.FAGSAK,
    val fagsakId: String? = null,
    val fagsaksystem: Fagsystem? = Fagsystem.KELVIN
)

data class JournalpostBruker(
    val id: String,
    val idType: String = "FNR"
)
