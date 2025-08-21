package no.nav.aap.postmottak.joarkavstemmer

import no.nav.aap.fordeler.RegelRepository
import no.nav.aap.postmottak.gateway.DoksikkerhetsnettGateway
import no.nav.aap.postmottak.gateway.GosysOppgaveGateway
import no.nav.aap.postmottak.gateway.JournalpostFraDoksikkerhetsnett
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

/**
 * Prøver å replikere logikken her: https://confluence.adeo.no/spaces/BOA/pages/366859456/doksikkerhetsnett
 */
class JoarkAvstemmer(
    private val doksikkerhetsnettGateway: DoksikkerhetsnettGateway,
    private val regelRepository: RegelRepository,
    private val gosysOppgaveGateway: GosysOppgaveGateway,
    private val journalpostGateway: JournalpostGateway,
    private val unleashGateway: UnleashGateway
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun avstem() {
        val eldreJournalpost = doksikkerhetsnettGateway.finnMottatteJournalposterEldreEnn(5)

        log.info("Fant ${eldreJournalpost.size} journalposter eldre enn 5 dager.")

        eldreJournalpost.forEach {
            avstemJournalPost(it)
        }
    }

    private fun avstemJournalPost(journalpost: JournalpostFraDoksikkerhetsnett) {
        val journalpostId = JournalpostId(journalpost.journalpostId)
        val regelResultat = regelRepository.hentRegelresultat(journalpostId)

        if (regelResultat == null) {
            log.error("Fant ikke regelresultat for journalpostId=$journalpostId. Har ikke nok informasjon til å fullføre. Dato opprettet: ${journalpost.datoOpprettet}.")
            return
        }

        if (regelResultat.skalTilKelvin()) {
            log.error("Fant ubehandlet journalpost eldre enn 5 dager som skal til Kelvin. ID: ${journalpostId}. Dato opprettet: ${journalpost.datoOpprettet}.")
        } else {
            val uthentet = journalpostGateway.hentJournalpost(journalpostId)
            val ident = uthentet.bruker?.id
            log.info("Fant ubehandlet journalpost. Oppretter Gosys-oppgave. JournalpostId: ${journalpostId}.")

            if (unleashGateway.isEnabled(PostmottakFeature.AvstemMotJoark)) {
                gosysOppgaveGateway.opprettJournalføringsOppgaveHvisIkkeEksisterer(
                    journalpostId = journalpostId,
                    personIdent = ident?.let(::Ident),
                    beskrivelse = "Automatisk gjenopprettet oppgave",
                    tildeltEnhetsnr = tildeltEnhetsnr(journalpost.journalforendeEnhet),
                    behandlingstema = journalpost.behandlingstema,
                )
            }
        }
    }

    private fun tildeltEnhetsnr(journalforendeEnhet: String?): String? {
        if (journalforendeEnhet == "9999") {
            return null
        }
        return journalforendeEnhet
    }
}