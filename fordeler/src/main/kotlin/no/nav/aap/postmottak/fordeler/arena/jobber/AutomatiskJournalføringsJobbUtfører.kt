package no.nav.aap.postmottak.fordeler.arena.jobber

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.postmottak.klient.joark.JoarkClient


class AutomatiskJournalføringsJobbUtfører(
    private val joarkClient: JoarkClient
) : JobbUtfører {

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return AutomatiskJournalføringsJobbUtfører(
                JoarkClient()
            )
        }

        override fun type() = "arena.oppgaveoppretter"

        override fun navn() = "Opprett oppgave i Arena"

        override fun beskrivelse() = "Oppretter oppgave i Arena for ny Søknad om AAP"

    }

    override fun utfør(input: JobbInput) {
        val kontekst = input.getAutomatiskJournalføringKontekst()

        joarkClient.førJournalpostPåFagsak(kontekst.journalpostId, kontekst.ident, kontekst.saksnummer)
        joarkClient.ferdigstillJournalpostMaskinelt(kontekst.journalpostId)

    }

}
