package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.HendelsesRepository
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Dokument

private const val ARENA_LEGEERKLÆRING_TEMA = "Oppfølging"

class ArenaVideresender(val connection: DBConnection) {

    private val arenaProducer = ArenaProducer(ProducerProvider.provideProducer(), HendelsesRepository(connection))
    private val journalpostService = JournalpostService.konstruer(connection)
    private val joarkClient = JoarkClient()
    
    
    fun videresendJournalpostTilArena(meldingId: String, journalpostId: JournalpostId) {
        val journalpost = journalpostService.hentjournalpost(journalpostId)

        if (journalpost.dokumenter.any { erLegeerklæring(it) }) {
            joarkClient.førJournalpostPåGenerellSak(journalpost, ARENA_LEGEERKLÆRING_TEMA)
            joarkClient.ferdigstillJournalpostMaskinelt(journalpost)
            // TODO: Send til Arena
        } else {
            arenaProducer.sendJournalpostTilArena(meldingId)
        }
    }

    private fun erLegeerklæring(dokument: Dokument): Boolean {
        return dokument.brevkode in listOf("NAV 08-07.08", "L9")
    }
    
    

}