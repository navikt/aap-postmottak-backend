package no.nav.aap.postmottak.prosessering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import org.slf4j.LoggerFactory

/**
 * JobbUtfører som håndterer feilregistrerte journalposter der alle behandlinger er avsluttet.
 *
 * Dersom journalposten har blitt overlevert til Kelvin (skalOverleveresTilKelvin == true),
 * sendes en feilregistrert-hendelse til behandlingsflyt.
 */
class FeilregistrertJournalpostJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val overleveringVurderingRepository: OverleveringVurderingRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway
) : JobbUtfører {

    private val log = LoggerFactory.getLogger(FeilregistrertJournalpostJobbUtfører::class.java)

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return FeilregistrertJournalpostJobbUtfører(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                gatewayProvider.provide()
            )
        }

        override val type = "feilregistrert.journalpost"

        override val navn = "Håndter feilregistrert journalpost"

        override val beskrivelse = "Sender feilregistrert-hendelse til behandlingsflyt dersom journalposten ble overlevert til Kelvin"
    }

    override fun utfør(input: JobbInput) {
        val journalpostId = input.getJournalpostId()
        log.info("Håndterer feilregistrert journalpost: $journalpostId")

        val behandlinger = behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
        val dokumentflytBehandling = behandlinger.find {
            it.typeBehandling == TypeBehandling.DokumentHåndtering
        }

        if (dokumentflytBehandling == null) {
            log.info("Fant ingen dokumentflyt-behandling for journalpost $journalpostId - ingen hendelse sendes")
            return
        }

        val overleveringVurdering = overleveringVurderingRepository.hentHvisEksisterer(dokumentflytBehandling.id)

        if (overleveringVurdering?.skalOverleveresTilKelvin != true) {
            log.info("Journalpost $journalpostId ble ikke overlevert til Kelvin - ingen hendelse sendes")
            return
        }

        val saksnummer = saksnummerRepository.hentSakVurdering(dokumentflytBehandling.id)?.saksnummer

        if (saksnummer == null) {
            log.warn("Fant ikke saksnummer for behandling ${dokumentflytBehandling.id} - kan ikke sende feilregistrert-hendelse")
            return
        }

        log.info("Sender feilregistrert-hendelse for journalpost $journalpostId til behandlingsflyt")
        behandlingsflytGateway.sendFeilregistrertHendelse(
            journalpostId = journalpostId,
            saksnummer = saksnummer
        )
    }
}

