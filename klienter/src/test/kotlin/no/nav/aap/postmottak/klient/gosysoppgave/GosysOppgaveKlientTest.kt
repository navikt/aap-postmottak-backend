package no.nav.aap.postmottak.klient.gosysoppgave

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime.of

@Fakes
class GosysOppgaveKlientTest {
    private lateinit var gosysOppgaveKlient: GosysOppgaveKlient

    @BeforeEach
    fun setUp() {
        PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        gosysOppgaveKlient = GosysOppgaveKlient()
    }

    @Test
    fun opprettEndreTemaOppgave() {
        val journalpostId = TestJournalposter.leggTil().journalpostId()

        gosysOppgaveKlient.opprettEndreTemaOppgaveHvisIkkeEksisterer(journalpostId, "YOLO", null)

        assertThat(antallGosysKall("finn_oppgaver")).isEqualTo(1.0)
        assertThat(antallGosysKall("opprett_oppgave")).isEqualTo(1.0)
    }

    @Test
    fun `ferdigstilling av oppgave telles som kall til Gosys`() {
        gosysOppgaveKlient.ferdigstillOppgave(1)

        assertThat(antallGosysKall("ferdigstill_oppgave")).isEqualTo(1.0)
    }

    //TODO: Forbedre denne testen
    @Test
    fun `når en journalpost alt har oppgaver skal det ikke opprettes en ny oppgave`() {
        val journalpostId = TestJournalposter.leggTil {
            harEksisterendeGosysOppgave = true
        }.journalpostId()

        gosysOppgaveKlient.opprettEndreTemaOppgaveHvisIkkeEksisterer(journalpostId, "YOLO", null)
        gosysOppgaveKlient.opprettJournalføringsOppgaveHvisIkkeEksisterer(
            journalpostId,
            Ident("YOLO"),
            "YOLO",
            "YOLO"
        )
        gosysOppgaveKlient.opprettFordelingsOppgaveHvisIkkeEksisterer(journalpostId, "YOLO", null, "YOLO")
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

    private fun antallGosysKall(operasjon: String): Double =
        PrometheusProvider.prometheus
            .get("postmottak_gosys_kall")
            .tag("operasjon", operasjon)
            .counter()
            .count()
}
