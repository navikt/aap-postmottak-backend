package no.nav.aap.postmottak.api.flyt.service

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory
import java.util.UUID

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

    fun Redigitaliser(journalpostReferanse: UUID) {
        val behandling = behandlingRepository.hent(Behandlingsreferanse(journalpostReferanse))
        requireNotNull(
            journalpostRepository.hentHvisEksisterer(behandling.id)
        ) { "Journalpost ikke funnet. Req: ${journalpostReferanse}." }

        val dokumentbehandlingId =
            behandlingRepository.opprettBehandling(
                behandling.journalpostId,
                TypeBehandling.DokumentHåndtering
            )

        log.info("Legger til jobb for redigitalisering")

        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(behandling.journalpostId.referanse, dokumentbehandlingId.id).medCallId()
        )
    }
}