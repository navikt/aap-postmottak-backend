package no.nav.aap.postmottak.klient.gosysoppgave

import no.bekk.bekkopen.date.NorwegianDateUtil.addWorkingDaysToDate
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalDateTime.now
import java.time.ZoneId.systemDefault
import java.util.*

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
    val fristFerdigstillelse: LocalDate? = null
)


fun finnStandardOppgavefrist(nå: LocalDateTime = now()): LocalDate {
    val SISTE_ARBEIDSTIME = 12
    fun Int.dagerTilFrist() = if (this < SISTE_ARBEIDSTIME) 1 else 2
    return with(nå)
    {
        addWorkingDaysToDate(
            Date.from(toLocalDate().atStartOfDay(systemDefault()).toInstant()),
            hour.dagerTilFrist()
        ).toInstant()
            .atZone(systemDefault()).toLocalDate()
    }
}

typealias NavEnhet = String

enum class Prioritet {
    HOY,
    NORM,
    LAV
}