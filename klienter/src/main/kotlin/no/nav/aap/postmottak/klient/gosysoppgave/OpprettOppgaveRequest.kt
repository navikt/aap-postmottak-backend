package no.nav.aap.postmottak.klient.gosysoppgave

import java.time.LocalDate

enum class Oppgavetype {
    JFR,
    FDR
}

data class OpprettOppgaveRequest(
    val oppgavetype: String, // se kodeverk
    val tema: String = "AAP", // se kodeverk
    val prioritet: Prioritet = Prioritet.NORM,
    val aktivDato: String = LocalDate.now().toString(), // dato
    val personident: String? = null, // 11 - 13 tegn
    val orgnr: String? = null,
    val tildeltEnhetsnr: NavEnhet? = null, // 4 tegn
    val opprettetAvEnhetsnr: NavEnhet? = "9999", // 4 tegn
    val journalpostId: String,
    val behandlesAvApplikasjon: String? = null,
    val tilordnetRessurs: String? = null, // navident
    val beskrivelse: String? = null,
    val behandlingstema: String? = null, // se kodeverk
    val behandlingstype: String? = null, // se kodeverk
    val fristFerdigstillelse: String? = null, // dato
)

typealias NavEnhet = String

enum class Statuskategori {
    AAPEN, AVSLUTTET
}

enum class Prioritet {
    HOY,
    NORM,
    LAV
}