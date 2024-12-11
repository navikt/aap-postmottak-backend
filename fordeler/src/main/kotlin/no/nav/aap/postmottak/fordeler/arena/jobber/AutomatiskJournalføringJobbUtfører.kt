package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.klient.joark.JoarkClient
import org.slf4j.LoggerFactory


class AutomatiskJournalføringJobbUtfører(
    private val joarkClient: JoarkClient
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return AutomatiskJournalføringJobbUtfører(
                JoarkClient()
            )
        }

        override fun type() = "arena.automatiskjournalføring"

        override fun navn() = "Automatisk journalfører"

        override fun beskrivelse() = "Journalfører journalposter som kan behandles automatisk"

    }

    private var log = LoggerFactory.getLogger(this::class.java)

    override fun utfør(input: JobbInput) {
        val kontekst = input.getAutomatiskJournalføringKontekst()

        log.info("Automatisk journalfører journalpost ${kontekst.journalpostId} på sak ${kontekst.saksnummer} ")

        joarkClient.førJournalpostPåFagsak(kontekst.journalpostId, kontekst.ident, kontekst.saksnummer)
        joarkClient.ferdigstillJournalpostMaskinelt(kontekst.journalpostId)
    }

}
