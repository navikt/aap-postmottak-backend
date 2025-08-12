package no.nav.aap.postmottak.gateway

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface JournalføringsGateway : Gateway {
    fun førJournalpostPåFagsak(
        journalpostId: JournalpostId,
        ident: Ident,
        fagsakId: String,
        tema: String = "AAP",
        fagsystem: Fagsystem = Fagsystem.KELVIN,
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null
    )

    fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String = "AAP",
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null
    )
    fun ferdigstillJournalpostMaskinelt(journalpostId: JournalpostId)
    fun ferdigstillJournalpost(journalpostId: JournalpostId, journalfoerendeEnhet: String)
}

data class FerdigstillRequest(
    val journalfoerendeEnhet: String
)

data class OppdaterJournalpostRequest(
    val behandlingstema: String? = null,
    val sak: JournalpostSak,
    val tema: String,
    val bruker: JournalpostBruker,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val tittel: String?,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val avsenderMottaker: AvsenderMottakerDto?,
    @param:JsonInclude(JsonInclude.Include.NON_NULL)
    val dokumenter: List<ForenkletDokument>?
)

data class AvsenderMottakerDto(
    val id: String,
    val idType: BrukerIdType,
    val navn: String? = null,
)

enum class Fagsystem {
    KELVIN,
    AO01, // Arena
    FS22 // Generell sak
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
