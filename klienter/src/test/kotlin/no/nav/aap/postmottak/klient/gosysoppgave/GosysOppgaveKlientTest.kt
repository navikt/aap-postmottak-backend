package no.nav.aap.postmottak.klient.gosysoppgave

import io.ktor.server.response.*
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.test.fakes.gosysOppgaveFake
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime.of


class ShouldNotBeCalledException(message: String = "This endpoint should not have been called") : Exception(message)

class GosysOppgaveKlientTest : WithFakes {

    @AfterEach
    fun tearDown() {
        WithFakes.fakes.gosysOppgave.clean()
    }

    val gosysOppgaveKlient = GosysOppgaveKlient()

    @Test
    fun opprettEndreTemaOppgave() {
        gosysOppgaveKlient.opprettEndreTemaOppgaveHvisIkkeEksisterer(JournalpostId(1), "YOLO")
    }

    @Test
    fun `når en journalpost alt har oppgaver skal det ikke opprettes en ny oppgave`() {
        WithFakes.fakes.gosysOppgave.setCustomModule {
            gosysOppgaveFake(
                getOppgaver = { call.respond(FinnOppgaverResponse(listOf(Oppgave(1)))) },
                postOppgave = { throw ShouldNotBeCalledException("Dette endepunktet skal ikke ha blitt kalt ettersom det alt finnes en oppgave") }
            )
        }
        gosysOppgaveKlient.opprettEndreTemaOppgaveHvisIkkeEksisterer(JournalpostId(1), "YOLO")
    }

    @Test
    fun `Standard oppgavefrist skal være én virkedag dersom arbeidstime er før time 12`() {
        val dagenFørSkjærtorsdag = of(2025, 4, 16, 11, 0, 0)
        val actual = finnStandardOppgavefrist(dagenFørSkjærtorsdag)
        val dagenEtterAndrePåskedag = LocalDate.of(2025, 4, 22)
        assertThat(actual).isEqualTo(dagenEtterAndrePåskedag)
    }

    @Test
    fun `Standard oppgavefrist skal være to virkedager dersom arbeidstime er etter time 12`() {
        val dagenFørSkjærtorsdag = of(2025, 4, 16, 13, 0, 0)
        val actual = finnStandardOppgavefrist(dagenFørSkjærtorsdag)
        val toDagerEtterAndrePåskedag = LocalDate.of(2025, 4, 23)
        assertThat(actual).isEqualTo(toDagerEtterAndrePåskedag)
    }
}
