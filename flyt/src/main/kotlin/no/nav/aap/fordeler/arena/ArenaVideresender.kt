package no.nav.aap.fordeler.arena

import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.arena.jobber.ArenaVideresenderKontekst
import no.nav.aap.fordeler.arena.jobber.ManuellJournalføringJobbUtfører
import no.nav.aap.fordeler.arena.jobber.OppprettOppgaveIArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendSøknadTilArenaJobbUtfører
import no.nav.aap.fordeler.arena.jobber.SendTilArenaKjørelisteBehandling
import no.nav.aap.fordeler.arena.jobber.medArenaVideresenderKontekst
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostService
import no.nav.aap.postmottak.gateway.JournalføringsGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Brevkoder
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.slf4j.LoggerFactory

private const val ARENA_LEGEERKLÆRING_TEMA = "OPP"
private val log = LoggerFactory.getLogger(ArenaVideresender::class.java)

class ArenaVideresender(
    val journalpostService: JournalpostService,
    val joarkClient: JournalføringsGateway,
    val flytJobbRepository: FlytJobbRepository,
    val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
) {
    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): ArenaVideresender {
            return ArenaVideresender(
                JournalpostService.konstruer(repositoryProvider, gatewayProvider),
                gatewayProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
            )
        }
    }

    fun videresendJournalpostTilArena(journalpostId: JournalpostId, innkommendeJournalpostId: Long) {
        val journalpost = journalpostService.hentJournalpost(journalpostId)

        if (journalpost.status == Journalstatus.JOURNALFOERT) {
            log.info("Journalposten er allerede journalført - oppretter ikke oppgaver i Arena eller gosys")
            return
        } else if (journalpost.status == Journalstatus.UTGAAR) {
            log.info("Journalposten er utgått - oppretter ikke oppgaver i Arena eller gosys")
            return
        }

        val videresenderKontekst = opprettArenaVideresenderKontekst(journalpost, innkommendeJournalpostId)
        if (videresenderKontekst.navEnhet == null) {
            sendTilManuellJournalføring(journalpost, innkommendeJournalpostId)
            return
        }

        when (journalpost.hoveddokumentbrevkode) {
            Brevkoder.LEGEERKLÆRING.kode -> {
                joarkClient.førJournalpostPåGenerellSak(journalpost, ARENA_LEGEERKLÆRING_TEMA)
                joarkClient.ferdigstillJournalpostMaskinelt(journalpost.journalpostId)
            }

            Brevkoder.SØKNAD.kode -> sendSøknadTilArena(journalpost, innkommendeJournalpostId)
            Brevkoder.STANDARD_ETTERSENDING.kode -> opprettOppagvePåEksisterendeSak(
                journalpost,
                innkommendeJournalpostId
            )

            Brevkoder.SØKNAD_OM_REISESTØNAD.kode -> opprettOppagvePåEksisterendeSak(
                journalpost,
                innkommendeJournalpostId
            )

            Brevkoder.SØKNAD_OM_REISESTØNAD_ETTERSENDELSE.kode -> sendTiArenaKjøreliste(
                journalpost,
                innkommendeJournalpostId
            )// Håndteres af jfr-arena
            else -> {
                sendTilManuellJournalføring(journalpost, innkommendeJournalpostId)
            }
        }
    }

    private fun sendSøknadTilArena(journalpost: Journalpost, innkomendeJournalpostId: Long) {
        flytJobbRepository.leggTil(
            JobbInput(SendSøknadTilArenaJobbUtfører).medArenaVideresenderKontekst(
                opprettArenaVideresenderKontekst(journalpost, innkomendeJournalpostId)
            )
        )
    }

    private fun sendTiArenaKjøreliste(journalpost: Journalpost, innkomendeJournalpostId: Long) {
        flytJobbRepository.leggTil(
            JobbInput(SendTilArenaKjørelisteBehandling).medArenaVideresenderKontekst(
                opprettArenaVideresenderKontekst(journalpost, innkomendeJournalpostId)
            )
        )
    }

    private fun opprettOppagvePåEksisterendeSak(journalpost: Journalpost, innkommendeJournalpostId: Long) {
        flytJobbRepository.leggTil(
            JobbInput(OppprettOppgaveIArenaJobbUtfører).medArenaVideresenderKontekst(
                opprettArenaVideresenderKontekst(journalpost, innkommendeJournalpostId = innkommendeJournalpostId)
            )
        )
    }

    private fun sendTilManuellJournalføring(journalpost: Journalpost, innkommendeJournalpostId: Long) {
        flytJobbRepository.leggTil(
            JobbInput(ManuellJournalføringJobbUtfører)
                .medArenaVideresenderKontekst(
                    opprettArenaVideresenderKontekst(
                        journalpost,
                        innkommendeJournalpostId = innkommendeJournalpostId
                    )
                )
        )
    }

    private fun opprettArenaVideresenderKontekst(
        journalpost: Journalpost,
        innkommendeJournalpostId: Long
    ): ArenaVideresenderKontekst {
        val enhet = innkommendeJournalpostRepository.hent(journalpost.journalpostId).enhet

        return ArenaVideresenderKontekst.fra(journalpost, enhet, innkommendeJournalpostId = innkommendeJournalpostId)
    }

}