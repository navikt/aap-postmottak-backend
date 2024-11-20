package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.HendelsesRepository
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.Brevkoder

private const val ARENA_LEGEERKLÆRING_TEMA = "OPP"

class ArenaVideresender(connection: DBConnection) {

    private val journalpostService = JournalpostService.konstruer(connection)
    private val joarkClient = JoarkClient()
    private val flytJobbRepository = FlytJobbRepository(connection)
    
    fun videresendJournalpostTilArena(meldingId: String, journalpostId: JournalpostId) {
        val journalpost = journalpostService.hentjournalpost(journalpostId)

        when (journalpost.hoveddokumentbrevkode) {
            Brevkoder.LEGEERKLØRING.kode -> {
                joarkClient.førJournalpostPåGenerellSak(journalpost, ARENA_LEGEERKLÆRING_TEMA)
                joarkClient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)
            }
            Brevkoder.SØKNAD.kode -> {
                TODO("Opprett SendSøknadTilArenaJobb")
            }
        }
    }

}