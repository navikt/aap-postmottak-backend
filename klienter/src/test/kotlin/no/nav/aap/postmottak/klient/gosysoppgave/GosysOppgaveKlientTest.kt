package no.nav.aap.postmottak.klient.gosysoppgave

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.Fakes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime.of

@Fakes
class GosysOppgaveKlientTest {
    init {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
    }

    val gosysOppgaveKlient = GosysOppgaveKlient()

    @Test
    fun opprettEndreTemaOppgave() {
        gosysOppgaveKlient.opprettEndreTemaOppgaveHvisIkkeEksisterer(JournalpostId(1), "YOLO")
    }

    //TODO: Forbedre denne testen
    @Test
    fun `når en journalpost alt har oppgaver skal det ikke opprettes en ny oppgave`() {
        gosysOppgaveKlient.opprettEndreTemaOppgaveHvisIkkeEksisterer(JournalpostId(128), "YOLO")
        gosysOppgaveKlient.opprettJournalføringsOppgaveHvisIkkeEksisterer(
            JournalpostId(128),
            Ident("YOLO"),
            "YOLO",
            "YOLO"
        )
        gosysOppgaveKlient.opprettFordelingsOppgaveHvisIkkeEksisterer(JournalpostId(128), "YOLO", null, "YOLO")
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
