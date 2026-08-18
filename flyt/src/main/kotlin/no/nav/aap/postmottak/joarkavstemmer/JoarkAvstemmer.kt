package no.nav.aap.postmottak.joarkavstemmer

import io.micrometer.core.instrument.MeterRegistry
import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.komponenter.httpklient.httpclient.error.BadRequestHttpResponsException
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.postmottak.gateway.DoksikkerhetsnettGateway
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalpostFraDoksikkerhetsnett
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.Oppgavetype
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.ubehandledeJournalposterCounter
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Prøver å replikere logikken her: [Confluence Doksikkerhetsnett](https://confluence.adeo.no/spaces/BOA/pages/366859456/doksikkerhetsnett)
 */
class JoarkAvstemmer(
    private val doksikkerhetsnettGateway: DoksikkerhetsnettGateway,
    private val regelRepository: RegelRepository,
    private val behandlingRepository: BehandlingRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val journalpostGateway: JournalpostGateway,
    private val meterRegistry: MeterRegistry
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun hentUavstemte(): List<UavstemtJournalpost> {
        val eldreJournalpost = doksikkerhetsnettGateway.finnMottatteJournalposterEldreEnn(5)

        log.info("Hentet ${eldreJournalpost.size} journalposter eldre enn 5 dager.")

        return eldreJournalpost
            .filter { it.mottaksKanal != KanalFraKodeverk.EESSI.name }
            .filter {
                regelRepository.hentRegelresultat(JournalpostId(it.journalpostId))
                    ?.gikkTilKelvin() == true
            }
            .map {
                val behandling = behandlingRepository.hentÅpenJournalføringsbehandling(JournalpostId(it.journalpostId))

                UavstemtJournalpost(
                    journalpostId = it.journalpostId,
                    behandlingReferanse = behandling?.referanse?.referanse,
                    datoOpprettet = it.datoOpprettet.toLocalDate(),
                    mottaksKanal = it.mottaksKanal,
                )
            }
    }

    fun avstem() {
        val eldreJournalpost = doksikkerhetsnettGateway.finnMottatteJournalposterEldreEnn(5)

        log.info("Fant ${eldreJournalpost.size} journalposter eldre enn 5 dager.")

        eldreJournalpost
            // Vi gjør samme filtreringen i no.nav.aap.postmottak.mottak.JoarkKafkaHandler.
            .filter { it.mottaksKanal != KanalFraKodeverk.EESSI.name }
            .forEach {
                avstemJournalPost(it)
            }
    }

    private fun loggUavstemt(melding: String, journalpostId: JournalpostId, kanal: String?, datoOpprettet: LocalDate) {
        val level = if (Miljø.erProd()) Level.ERROR else Level.INFO
        val msg = "$melding Kanal: $kanal. JournalpostId: $journalpostId. Dato opprettet: $datoOpprettet"
        return log.makeLoggingEventBuilder(level).setMessage(msg).log()
    }

    private fun avstemJournalPost(journalpost: JournalpostFraDoksikkerhetsnett) {
        val journalpostId = JournalpostId(journalpost.journalpostId)
        val regelResultat = regelRepository.hentRegelresultat(journalpostId)

        val finnesEksisterendeOppgaverForJournalpost = finnesEksisterendeOppgaverForJournalpost(journalpostId)
        if (finnesEksisterendeOppgaverForJournalpost && regelResultat == null) {
            log.info("Finnes eksisterende Gosys-oppgave for journalpostId=$journalpostId, og har heller ikke regelresultat. Avbryter.")
            return
        }

        if (regelResultat == null && eldreEnnKelvin(journalpost)) {
            log.info("Fant ikke regelresultat for journalpostId=$journalpostId. Har ikke nok informasjon til å fullføre. Dato opprettet: ${journalpost.datoOpprettet}. Kanal: ${journalpost.mottaksKanal}.")
            return
        } else if (regelResultat == null) {
            meterRegistry.ubehandledeJournalposterCounter("UKJENT").increment()
            loggUavstemt(
                "Fant ikke regelresultat for journalpostId. Har ikke nok informasjon til å fullføre. Oppretter fordelingsoppgave.",
                journalpostId,
                journalpost.mottaksKanal,
                journalpost.datoOpprettet.toLocalDate()
            )
            val uthentet = journalpostGateway.hentJournalpost(journalpostId)
            val ident = uthentet.bruker?.id
            gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                journalpostId = journalpostId,
                personIdent = ident?.let(::Ident),
                beskrivelse = "Manglende journalføring - ${uthentet.tittel}",
            )
        } else if (regelResultat.gikkTilKelvin()) {
            meterRegistry.ubehandledeJournalposterCounter("KELVIN").increment()
            loggUavstemt(
                "Fant ubehandlet journalpost eldre enn 5 dager som skal til Kelvin.",
                journalpostId,
                journalpost.mottaksKanal,
                journalpost.datoOpprettet.toLocalDate()
            )
        } else if (!finnesEksisterendeOppgaverForJournalpost) {
            val uthentet = journalpostGateway.hentJournalpost(journalpostId)
            val ident = uthentet.bruker?.id
            log.info("Fant ubehandlet journalpost som ikke skal til Kelvin. Oppretter Gosys-oppgave. JournalpostId: ${journalpostId}. Systemnavn: ${regelResultat.systemNavn}. Alder på journalpost: ${journalpost.datoOpprettet}")

            meterRegistry.ubehandledeJournalposterCounter("ARENA").increment()
            try {
                gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                    journalpostId = journalpostId,
                    personIdent = ident?.let(::Ident),
                    beskrivelse = "Automatisk gjenopprettet oppgave",
                    tildeltEnhetsnr = tildeltEnhetsnr(journalpost.journalforendeEnhet),
                    behandlingstema = journalpost.behandlingstema,
                )
            } catch (e: BadRequestHttpResponsException) {
                try {
                    log.info("Feilet opprettelse av Gosys-oppgave for journalpostId=$journalpostId. Forsøk 1 feilet. Forsøk nr 2 med tildeltEnhetsnr=null og behandlingstema=null. Exception: ${e.message}.")
                    gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                        journalpostId = journalpostId,
                        personIdent = ident?.let(::Ident),
                        beskrivelse = "Automatisk gjenopprettet oppgave. Forsøk 1 feilet.",
                        tildeltEnhetsnr = null,
                        behandlingstema = null,
                    )
                } catch (e: BadRequestHttpResponsException) {
                    log.info("Feilet på tredje forsøk. Lager fordelingsoppgave. Ident-null: ${ident == null}.")
                    gosysOppgaveGateway.opprettFordelingsOppgaveHvisIkkeEksisterer(
                        journalpostId = journalpostId,
                        personIdent = ident?.let(::Ident),
                        beskrivelse = "Manglende journalføring - ${uthentet.tittel}",
                    )
                    log.warn("Kunne ikke opprette Gosys-oppgave for journalpostId=$journalpostId.", e)
                }
            }
            log.info("Opprettet Gosys-oppgave for journalpostId=$journalpostId.")
        } else {
            meterRegistry.ubehandledeJournalposterCounter("ARENA").increment()
            log.info("Det finnes allerede en Gosys-oppgave for journalpostId=$journalpostId. Dato opprettet: ${journalpost.datoOpprettet}. Avbryter.")
        }
    }

    private fun eldreEnnKelvin(journalpost: JournalpostFraDoksikkerhetsnett): Boolean =
        journalpost.datoOpprettet.isBefore(
            OffsetDateTime.of(
                LocalDate.of(2025, 4, 1).atStartOfDay(), ZoneOffset.UTC
            )
        )

    private fun finnesEksisterendeOppgaverForJournalpost(journalpostId: JournalpostId): Boolean {
        return gosysOppgaveGateway.finnOppgaverForJournalpost(
            journalpostId = journalpostId, tema = "AAP", oppgavetyper = listOf(
                Oppgavetype.JOURNALFØRING
            )
        ).isNotEmpty()
    }

    private fun tildeltEnhetsnr(journalforendeEnhet: String?): String? {
        if (journalforendeEnhet == "9999") {
            return null
        }
        return journalforendeEnhet
    }
}

data class UavstemtJournalpost(
    val journalpostId: Long,
    val behandlingReferanse: UUID?,
    val datoOpprettet: LocalDate,
    val mottaksKanal: String?,
)