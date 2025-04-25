package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface GosysOppgaveGateway : Gateway {
    fun opprettEndreTemaOppgaveHvisIkkeEksisterer(journalpostId: JournalpostId, personident: String)
    fun finnOppgaverForJournalpost(
        journalpostId: JournalpostId, 
        oppgavetyper: List<Oppgavetype> = listOf(Oppgavetype.JOURNALFØRING),
        tema: String?,
        statuskategori: Statuskategori = Statuskategori.AAPEN
    ): List<Long>

    fun ferdigstillOppgave(oppgaveId: Long)
    fun opprettJournalføringsOppgaveHvisIkkeEksisterer(
        journalpostId: JournalpostId,
        personIdent: Ident,
        beskrivelse: String,
        tildeltEnhetsnr: String
    )

    fun opprettFordelingsOppgaveHvisIkkeEksisterer(journalpostId: JournalpostId, orgnr: String? = null, personIdent: Ident?, beskrivelse: String)
}

enum class Oppgavetype(val verdi: String) {
    JOURNALFØRING("JFR"),
    FORDELING("FDR")
}

enum class Statuskategori {
    AAPEN, AVSLUTTET
}