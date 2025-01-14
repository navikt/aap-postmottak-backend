package no.nav.aap.postmottak.klient.gosysoppgave

import io.ktor.server.response.*
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.test.fakes.gosysOppgaveFake
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test


class ShouldNotBeCalledException(message: String = "This endpoint should not have been called") : Exception(message)

class GosysOppgaveKlientTest : WithFakes {

    @AfterEach
    fun tearDown() {
        WithFakes.fakes.gosysOppgave.clean()
    }

    val gosysOppgaveKlient = GosysOppgaveKlient()

    @Test
    fun opprettEndreTemaOppgave() {
        gosysOppgaveKlient.opprettEndreTemaOppgave(JournalpostId(1), "YOLO")
    }

    @Test
    fun `når en journalpost alt har oppgaver skal det ikke opprettes en ny oppgave`() {
        WithFakes.fakes.gosysOppgave.setCustomModule { gosysOppgaveFake(
            getOppgaver = {call.respond(FinnOppgaverResponse(listOf(Oppgave(1))))},
            postOppgave = { throw ShouldNotBeCalledException("Dette endepunktet skal ikke ha blitt kalt ettersom det alt finnes en oppgave")}
            ) }

        gosysOppgaveKlient.opprettEndreTemaOppgave(JournalpostId(1), "YOLO")
    }
}