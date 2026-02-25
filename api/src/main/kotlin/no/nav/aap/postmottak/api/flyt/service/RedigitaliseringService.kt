package no.nav.aap.postmottak.api.flyt.service

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory

class RedigitaliseringService(
    private val flytJobbRepository: FlytJobbRepository,
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository
) {
    val log = LoggerFactory.getLogger(RedigitaliseringService::class.java)

    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider): RedigitaliseringService {
            return RedigitaliseringService(
                flytJobbRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                journalpostRepository = repositoryProvider.provide(),
                saksnummerRepository = repositoryProvider.provide()
            )
        }
    }

    fun redigitaliser(journalpostId: Long, saksnummer: String) {
        val journalpost = journalpostRepository.hentHvisEksisterer(JournalpostId(journalpostId))
        requireNotNull(journalpost) { "Journalpost ikke funnet. Req: ${journalpostId}." }

        val behandling = behandlingRepository.hent(journalpost.journalpostId)
        requireNotNull(behandling) { "Behandling ikke funnet. Req: ${journalpost.journalpostId.referanse}." }

        val eksisterendeSak = saksnummerRepository.hentSakVurdering(behandling.id)
        requireNotNull(eksisterendeSak) { "Det må eksistere en sak fra før. Req: ${saksnummer}." }
        require(eksisterendeSak.saksnummer == saksnummer) { "Saksnummer innsendt avviker fra registrert saksnummer i postmottak. Req: ${saksnummer}." }

        val nyBehandlingId =
            behandlingRepository.opprettBehandling(
                journalpost.journalpostId,
                TypeBehandling.DokumentHåndtering
            )

        saksnummerRepository.lagreSakVurdering(nyBehandlingId, Saksvurdering(saksnummer, false))

        log.info("Legger til jobb for redigitalisering")

        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(nyBehandlingId.id, nyBehandlingId.id).medCallId()
        )
    }
}