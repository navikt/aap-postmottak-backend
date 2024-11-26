package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.Enhetsutreder
import no.nav.aap.postmottak.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.postmottak.fordeler.arena.jobber.SendSøknadTilArenaJobb
import no.nav.aap.postmottak.fordeler.arena.jobber.medArenaVideresenderKontekst
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.verdityper.Brevkoder

private const val ARENA_LEGEERKLÆRING_TEMA = "OPP"

class ArenaVideresender(connection: DBConnection) {

    private val journalpostService = JournalpostService.konstruer(connection)
    private val joarkClient = JoarkClient()
    private val flytJobbRepository = FlytJobbRepository(connection)
    private val enhetsutreder = Enhetsutreder(
        NorgKlient(),
        PdlGraphQLClient.withClientCredentialsRestClient(),
        NomKlient()
    )
    
    fun videresendJournalpostTilArena(meldingId: String, journalpostId: JournalpostId) {
        val journalpost = journalpostService.hentjournalpost(journalpostId)

        when (journalpost.hoveddokumentbrevkode) {
            Brevkoder.LEGEERKLØRING.kode -> {
                joarkClient.førJournalpostPåGenerellSak(journalpost, ARENA_LEGEERKLÆRING_TEMA)
                joarkClient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)
            }
            Brevkoder.SØKNAD.kode -> {
                sendJSøknadTilArena(journalpost)
            }
        }
    }

    private fun sendJSøknadTilArena(journalpost: JournalpostMedDokumentTitler) {
        val enhet = enhetsutreder.finnNavenhetForJournalpost(journalpost)
        flytJobbRepository.leggTil(
            JobbInput(SendSøknadTilArenaJobb).medArenaVideresenderKontekst(
                ArenaVideresenderKontekst(
                    journalpostId = journalpost.journalpostId,
                    ident = journalpost.person.aktivIdent(),
                    navEnhet = enhet,
                    hoveddokumenttittel = journalpost.getHoveddokumenttittel(),
                    vedleggstittler = journalpost.getVedleggTitler()
                )
            )
        )
    }

}