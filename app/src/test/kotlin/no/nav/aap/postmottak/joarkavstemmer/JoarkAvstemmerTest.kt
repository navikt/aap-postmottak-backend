package no.nav.aap.postmottak.joarkavstemmer

import ch.qos.logback.classic.Level.ERROR
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.runs
import io.mockk.verify
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.fordeler.Regelresultat
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.postmottak.gateway.DoksikkerhetsnettGateway
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalpostFraDoksikkerhetsnett
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Journalposttype
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(MockKExtension::class)
class JoarkAvstemmerTest {
    @MockK
    private lateinit var doksikkerhetsnettGateway: DoksikkerhetsnettGateway

    @MockK
    private lateinit var regelRepository: RegelRepository

    @MockK
    private lateinit var gosysOppgaveGateway: GosysOppgaveGateway

    @MockK
    private lateinit var journalpostGateway: JournalpostGateway

    private var logger = LoggerFactory.getLogger(JoarkAvstemmer::class.java) as Logger

    @BeforeEach
    fun beforeEach() {
        every {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } just runs

        val journalpostFraDoksikkerhetsnetts = listOf(
            journalpostFraDoksikkerhetsnett()
        )
        every { doksikkerhetsnettGateway.finnMottatteJournalposterEldreEnn(any()) } returns journalpostFraDoksikkerhetsnetts
        every { journalpostGateway.hentJournalpost(any()) } returns journalpost(journalpostFraDoksikkerhetsnetts.first().journalpostId)

        System.setProperty("NAIS_CLUSTER_NAME", "NAIS-PROD")
    }

    @Test
    fun `om det finnes gosys-oppgaver og journalposten ikke har vært innom kelvin, `() {
        every { gosysOppgaveGateway.finnOppgaverForJournalpost(any(), any(), any(), any()) } returns listOf(1L)
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns null

        verify(exactly = 0) {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `om det ikke finnes regelresultat, logges feil`() {
        every { gosysOppgaveGateway.finnOppgaverForJournalpost(any(), any(), any(), any()) } returns listOf()
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns null
        val listAppender = opprettListAppender()

        joarkAvstemmer().avstem()

        val loggMeldinger = listAppender.list

        assertThat(loggMeldinger).anySatisfy { loggMelding ->
            assertThat(loggMelding.formattedMessage).contains("Fant ikke regelresultat for journalpost")
            assertThat(loggMelding.level).isEqualTo(ERROR)
        }
    }

    @Test
    fun `for journalposter som skal behandles i kelvin, logges error`() {
        every { gosysOppgaveGateway.finnOppgaverForJournalpost(any(), any(), any(), any()) } returns listOf()
        val regelresultat = Regelresultat(mapOf(), 1, "KELVIN")
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns regelresultat

        val listAppender = opprettListAppender()

        joarkAvstemmer().avstem()

        val loggMeldinger = listAppender.list

        assertThat(loggMeldinger).anySatisfy { loggMelding ->
            assertThat(loggMelding.formattedMessage).contains("Fant ubehandlet journalpost eldre enn 5 dager som skal til Kelvin")
        }
    }

    @Test
    fun `oppretter oppgave i gosys for gamle journalposter som skal til Arena`() {
        val regelresultat = Regelresultat(mapOf(), 1, "ARENA")
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns regelresultat
        every { gosysOppgaveGateway.finnOppgaverForJournalpost(any(), any(), any(), any()) } returns listOf()

        every { journalpostGateway.hentJournalpost(any()) } returns journalpost(1)

        val listAppender = opprettListAppender()

        val avstemmer = joarkAvstemmer()

        avstemmer.avstem()

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                any(), any(), any(), any(), any()
            )
        }

        val loggMeldinger = listAppender.list

        assertThat(loggMeldinger).anySatisfy { loggMelding ->
            assertThat(loggMelding.formattedMessage).contains("Oppretter Gosys-oppgave.")
        }
    }

    @Test
    fun `om det feiler å opprette gosys-oppgave forsøker man en gang til med oppgavetype og tildeltenhetsnr lik null`() {
        val regelresultat = Regelresultat(mapOf(), 1, "ARENA")
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns regelresultat
        every { gosysOppgaveGateway.finnOppgaverForJournalpost(any(), any(), any(), any()) } returns listOf()

        every { journalpostGateway.hentJournalpost(any()) } returns journalpost(1)

        every {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                JournalpostId(1),
                any(),
                any(),
                any(),
                any()
            )
        } throws BadRequestHttpResponsException("Noe feilet")
        every {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                JournalpostId(1),
                any(),
                any(),
                null,
                null
            )
        } just runs

        val listAppender = opprettListAppender()

        val avstemmer = joarkAvstemmer()

        avstemmer.avstem()

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                any(), any(), any(), any(), "AAP"
            )
        }

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                any(), any(), any(), null, null,
            )
        }

        val loggMeldinger = listAppender.list

        assertThat(loggMeldinger).anySatisfy { loggMelding ->
            assertThat(loggMelding.formattedMessage).contains("Oppretter Gosys-oppgave.")
        }
    }

    @Test
    fun `om opprettelse av journalføringsoppgave feiler, lages det fordelingsoppgave`() {
        val regelresultat = Regelresultat(mapOf(), 1, "ARENA")
        every { regelRepository.hentRegelresultat(any<JournalpostId>()) } returns regelresultat
        every { gosysOppgaveGateway.finnOppgaverForJournalpost(any(), any(), any(), any()) } returns listOf()
        every { gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(any(), any(), any(), any()) } just runs

        every {
            gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                JournalpostId(1),
                any(),
                any(),
                any(),
                any()
            )
        } throws BadRequestHttpResponsException("Noe feilet")

        val listAppender = opprettListAppender()

        val avstemmer = joarkAvstemmer()

        avstemmer.avstem()

        verify(exactly = 1) {
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                any(), any(), any(), any()
            )
        }
        val loggMeldinger = listAppender.list

        assertThat(loggMeldinger).anySatisfy { loggMelding ->
            assertThat(loggMelding.formattedMessage).contains("Feilet på tredje forsøk. Lager fordelingsoppgave.")
        }
    }

    private fun joarkAvstemmer(): JoarkAvstemmer = JoarkAvstemmer(
        doksikkerhetsnettGateway = doksikkerhetsnettGateway,
        regelRepository = regelRepository,
        gosysOppgaveGateway = gosysOppgaveGateway,
        journalpostGateway = journalpostGateway,
        unleashGateway = object : UnleashGateway {
            override fun isEnabled(featureToggle: FeatureToggle): Boolean = true
        },
        meterRegistry = SimpleMeterRegistry()
    )

    private fun opprettListAppender(): ListAppender<ILoggingEvent> {
        val listAppender = ListAppender<ILoggingEvent>()
        logger.addAppender(listAppender)
        listAppender.start()
        return listAppender
    }

    private val counter = AtomicInteger(0)
    private fun journalpostFraDoksikkerhetsnett(): JournalpostFraDoksikkerhetsnett {
        return JournalpostFraDoksikkerhetsnett(
            journalpostId = counter.incrementAndGet().toLong(),
            journalStatus = "XXX",
            mottaksKanal = "XXX",
            tema = "AAP",
            behandlingstema = "AAP",
            journalforendeEnhet = "FFFF",
            datoOpprettet = OffsetDateTime.now().minusDays(10)
        )
    }

    private fun journalpost(id: Long): SafJournalpost {
        return SafJournalpost(
            journalpostId = id,
            tittel = "TODO",
            journalposttype = Journalposttype.I,
            journalstatus = Journalstatus.MOTTATT,
            tema = "AAP",
            temanavn = "Arbeidsavklaringspenger",
            behandlingstema = "xxxx",
            behandlingstemanavn = "xxxx",
            sak = null,
            bruker = null,
            avsenderMottaker = null,
            journalfoerendeEnhet = null,
            kanal = KanalFraKodeverk.UKJENT,
            eksternReferanseId = null,
            relevanteDatoer = null,
            dokumenter = null,
        )
    }
}