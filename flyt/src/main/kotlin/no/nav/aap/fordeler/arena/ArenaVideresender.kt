package no.nav.aap.fordeler.arena

import no.nav.aap.fordeler.Enhetsutreder
import no.nav.aap.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.OppprettOppgaveIArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendTilArenaKjørelisteBehandling
import no.nav.aap.fordeler.arena.jobber.medArenaVideresenderKontekst
import no.nav.aap.fordeler.arena.jobber.opprettArenaVideresenderKontekst
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

private const val ARENA_LEGEERKLÆRING_TEMA = "OPP"
private val log = LoggerFactory.getLogger(ArenaVideresender::class.java)

class ArenaVideresender(
    val journalpostService: JournalpostService,
    val joarkClient: JournalføringsGateway,
    val flytJobbRepository: FlytJobbRepository,
    val enhetsutreder: Enhetsutreder,
) {
    companion object {
        fun konstruer(connection: DBConnection) = ArenaVideresender(
            JournalpostService.konstruer(connection),
            GatewayProvider.provide(JournalføringsGateway::class),
            FlytJobbRepository(connection),
            Enhetsutreder.konstruer(),
        )
    }

    fun videresendJournalpostTilArena(journalpostId: JournalpostId) {
        val journalpost = journalpostService.hentJournalpostMedDokumentTitler(journalpostId)
        
        if (journalpost.status == Journalstatus.JOURNALFOERT) {
            log.info("Journalposten er allerede journalført - oppretter ikke oppgaver i Arena eller gosys")
            return
        } else if (journalpost.status == Journalstatus.UTGAAR) {
            log.info("Journalposten er utgått - oppretter ikke oppgaver i Arena eller gosys")
            return
        }
        
        val videresenderKontekst = opprettArenaVideresenderKontekst(journalpost)
        if (videresenderKontekst.navEnhet == null) {
            sendTilManuellJournalføring(journalpost)
            return
        }

        when (journalpost.hoveddokumentbrevkode) {
            Brevkoder.LEGEERKLÆRING.kode -> {
                joarkClient.førJournalpostPåGenerellSak(journalpost, ARENA_LEGEERKLÆRING_TEMA)
                joarkClient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)
            }

            Brevkoder.SØKNAD.kode -> sendSøknadTilArena(journalpost)
            Brevkoder.STANDARD_ETTERSENDING.kode -> opprettOppagvePåEksisterendeSak(journalpost)
            Brevkoder.SØKNAD_OM_REISESTØNAD.kode -> opprettOppagvePåEksisterendeSak(journalpost)
            Brevkoder.SØKNAD_OM_REISESTØNAD_ETTERSENDELSE.kode -> sendTiArenaKjøreliste(journalpost)// Håndteres af jfr-arena
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

    private fun sendTiArenaKjøreliste(journalpost: JournalpostMedDokumentTitler) {
        flytJobbRepository.leggTil(
            JobbInput(SendTilArenaKjørelisteBehandling).medArenaVideresenderKontekst(
                opprettArenaVideresenderKontekst(journalpost)
            )
        )
    }

    private fun opprettOppagvePåEksisterendeSak(journalpost: JournalpostMedDokumentTitler) {
        flytJobbRepository.leggTil(
            JobbInput(OppprettOppgaveIArenaJobbUtfører).medArenaVideresenderKontekst(
                opprettArenaVideresenderKontekst(journalpost)
            )
        )
    }

    private fun sendTilManuellJournalføring(journalpost: JournalpostMedDokumentTitler) {
        flytJobbRepository.leggTil(
            JobbInput(ManuellJournalføringJobbUtfører)
                .medArenaVideresenderKontekst(opprettArenaVideresenderKontekst(journalpost))
        )
    }

    private fun opprettArenaVideresenderKontekst(journalpost: JournalpostMedDokumentTitler): ArenaVideresenderKontekst {
        val enhet = enhetsutreder.finnJournalføringsenhet(journalpost)
        return journalpost.opprettArenaVideresenderKontekst(enhet)
    }

}