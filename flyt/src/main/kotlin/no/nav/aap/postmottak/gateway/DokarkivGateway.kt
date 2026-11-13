package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.komponenter.verdityper.Bruker
import no.nav.aap.postmottak.avklaringsbehov.løsning.ForenkletDokument
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.util.UUID

interface DokarkivGateway : Gateway {
    fun endreTema(journalpostId: JournalpostId, tema: String, endretAv: Bruker?)

    fun førJournalpostPåFagsak(
        journalpostId: JournalpostId,
        ident: Ident,
        fagsakId: String,
        tema: String = "AAP",
        fagsystem: Fagsystem = Fagsystem.KELVIN,
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null,
        endretAv: Bruker?,
    )

    fun førJournalpostPåGenerellSak(
        journalpost: Journalpost,
        tema: String = "AAP",
        tittel: String? = null,
        avsenderMottaker: AvsenderMottakerDto? = null,
        dokumenter: List<ForenkletDokument>? = null,
        endretAv: Bruker?,
    )

    fun kopierJournalpost(journalpost: Journalpost): JournalpostId

    fun opprettJournalpostForPerson(
        ident: Ident,
        pdf: ByteArray,
        filNavn: String,
        eksternReferanseId: UUID?,
    ): JournalpostId

    fun ferdigstillJournalpost(
        journalpostId: JournalpostId,
        journalfoerendeEnhet: String,
        journalførtAv: Bruker?,
    )
}