package no.nav.aap.postmottak.fordeler.arena

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.fordeler.Enhetsutreder
import no.nav.aap.postmottak.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.postmottak.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.AutomatiskJournalføringKontekst
import no.nav.aap.postmottak.fordeler.arena.jobber.AutomatiskJournalføringJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.postmottak.fordeler.arena.jobber.medArenaVideresenderKontekst
import no.nav.aap.postmottak.fordeler.arena.jobber.medAutomatiskJournalføringKontekst
import no.nav.aap.postmottak.klient.arena.ArenaKlient
import no.nav.aap.postmottak.klient.joark.JoarkClient
import no.nav.aap.postmottak.klient.nom.NomKlient
import no.nav.aap.postmottak.klient.norg.NorgKlient
import no.nav.aap.postmottak.klient.pdl.PdlGraphQLClient
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.sakogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.verdityper.Brevkoder

private const val ARENA_LEGEERKLÆRING_TEMA = "OPP"

class ArenaVideresender(
    val journalpostService: JournalpostService,
    val joarkClient: JoarkClient,
    val flytJobbRepository: FlytJobbRepository,
    val enhetsutreder: Enhetsutreder,
    val arenaKlient: ArenaKlient
) {
    companion object {
        fun konstruer(connection: DBConnection) = ArenaVideresender(
            JournalpostService.konstruer(connection),
            JoarkClient(),
            FlytJobbRepository(connection),
            Enhetsutreder(
                NorgKlient(),
                PdlGraphQLClient.withClientCredentialsRestClient(),
                NomKlient()
            ),
            ArenaKlient()
        )
    }

    fun videresendJournalpostTilArena(journalpostId: JournalpostId) {
        val journalpost = journalpostService.hentjournalpost(journalpostId)

        when (journalpost.hoveddokumentbrevkode) {
            Brevkoder.LEGEERKLÆRING.kode -> {
                joarkClient.førJournalpostPåGenerellSak(journalpost, ARENA_LEGEERKLÆRING_TEMA)
                joarkClient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)
            }
            Brevkoder.SØKNAD.kode -> {
                sendSøknadTilArena(journalpost)
            }
            Brevkoder.STANDARD_ETTERSENDING.kode -> {
                sendSøknadsettersendelseTilArena(journalpost)
            }
            else -> {
                sendTilManuellJournalføring(journalpost)
            }
        }
    }

    private fun sendSøknadTilArena(journalpost: JournalpostMedDokumentTitler) {
        flytJobbRepository.leggTil(
            JobbInput(SendSøknadTilArenaJobbUtfører).medArenaVideresenderKontekst(
                opprettArenaVideresenderKontekst(journalpost)
            )
        )
    }

    private fun sendSøknadsettersendelseTilArena(journalpost: Journalpost) {
        val saksId = arenaKlient.nyesteAktiveSak(journalpost.person.aktivIdent()) ?: error("Fant ikke arenasaksnummer")
        flytJobbRepository.leggTil(JobbInput(AutomatiskJournalføringJobbUtfører).medAutomatiskJournalføringKontekst(
            AutomatiskJournalføringKontekst(
                journalpostId = journalpost.journalpostId,
                ident = journalpost.person.aktivIdent(),
                saksnummer = saksId
            )
        ))
    }

    private fun sendTilManuellJournalføring(journalpost: JournalpostMedDokumentTitler) {
        flytJobbRepository.leggTil(JobbInput(ManuellJournalføringJobbUtfører)
            .medArenaVideresenderKontekst(opprettArenaVideresenderKontekst(journalpost)))
    }

    private fun opprettArenaVideresenderKontekst (journalpost: JournalpostMedDokumentTitler) = ArenaVideresenderKontekst(
        journalpostId = journalpost.journalpostId,
        ident = journalpost.person.aktivIdent(),
        navEnhet = enhetsutreder.finnNavenhetForJournalpost(journalpost),
        hoveddokumenttittel = journalpost.getHoveddokumenttittel(),
        vedleggstitler = journalpost.getVedleggTitler()
    )

}