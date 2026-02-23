package no.nav.aap.postmottak.api.flyt.service

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory

class RedigitaliseringService(
    private val flytJobbRepository: FlytJobbRepository,
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository
) {
    val log = LoggerFactory.getLogger(RedigitaliseringService::class.java)

    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider): RedigitaliseringService {
            return RedigitaliseringService(
                flytJobbRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                journalpostRepository = repositoryProvider.provide()
            )
        }
    }

    fun Redigitaliser(journalpostId: Long) {
        val journalpost = journalpostRepository.hentHvisEksisterer(JournalpostId(journalpostId))

        requireNotNull(
            journalpost
        ) { "Journalpost ikke funnet. Req: ${journalpostId}." }

        val dokumentbehandlingId =
            behandlingRepository.opprettBehandling(
                journalpost.journalpostId,
                TypeBehandling.DokumentHåndtering
            )

        log.info("Legger til jobb for redigitalisering")

        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(journalpost.journalpostId.referanse, dokumentbehandlingId.id).medCallId()
        )
    }
}