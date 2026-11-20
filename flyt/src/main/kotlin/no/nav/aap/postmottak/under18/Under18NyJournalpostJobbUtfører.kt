package no.nav.aap.postmottak.under18

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.getJournalpostId
import no.nav.aap.postmottak.prosessering.medJournalpostId
import org.slf4j.LoggerFactory

/**
 * Tar under-18-behandlingen av automatisk samtykkevent når en ny journalpost på søkeren er mottatt
 * etter at venten ble satt. Journalposter som allerede var mottatt, tar ikke behandlingen av vent på nytt.
 */
class Under18NyJournalpostJobbUtfører(
    private val behandlingRepository: BehandlingRepository,
    private val avklaringsbehovRepository: AvklaringsbehovRepository,
    private val samtykkekandidater: Under18Samtykkekandidater,
    private val avklaringsbehovOrkestrator: AvklaringsbehovOrkestrator,
    private val flytJobbRepository: FlytJobbRepository,
) : JobbUtfører {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun utfør(input: JobbInput) {
        val behandling = behandlingRepository.hent(BehandlingId(input.behandlingId()))
        val journalpostId = input.getJournalpostId()

        val vent = avklaringsbehovRepository.hentAvklaringsbehovene(behandling.id)
            .hentVentepunkter()
            .singleOrNull { it.definisjon == SAMTYKKEVENT }
        if (vent == null) {
            log.info("Behandling ${behandling.id} står ikke på $SAMTYKKEVENT. Ny journalpost $journalpostId gir ingen endring.")
            return
        }
        val ventSatt = vent.historikk.filter { it.status == Status.OPPRETTET }.maxOf { it.tidsstempel }
        val kandidat = samtykkekandidater.hent(behandling).singleOrNull { it.journalpostId == journalpostId }
        if (kandidat == null || kandidat.mottattTid <= ventSatt) {
            log.info("Journalpost $journalpostId er ingen ny samtykkekandidat for behandling ${behandling.id}.")
            return
        }

        avklaringsbehovOrkestrator.taAvVent(behandling.id, SAMTYKKEVENT, "Ny journalpost $journalpostId er mottatt.")
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(sakID = behandling.journalpostId.referanse, behandlingId = behandling.id.id)
                .medCallId()
        )
    }

    companion object : ProvidersJobbSpesifikasjon {
        private val SAMTYKKEVENT = Definisjon.VENT_PA_UNDER18_SAMTYKKE

        fun jobbInput(behandling: Behandling, nyJournalpostId: JournalpostId): JobbInput =
            JobbInput(Under18NyJournalpostJobbUtfører)
                .forBehandling(sakID = behandling.journalpostId.referanse, behandlingId = behandling.id.id)
                .medJournalpostId(nyJournalpostId)
                .medCallId()

        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører =
            Under18NyJournalpostJobbUtfører(
                behandlingRepository = repositoryProvider.provide(),
                avklaringsbehovRepository = repositoryProvider.provide(),
                samtykkekandidater = Under18Samtykkekandidater.konstruer(repositoryProvider),
                avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(repositoryProvider, gatewayProvider),
                flytJobbRepository = repositoryProvider.provide(),
            )

        override val type = "under18.nyJournalpost"
        override val navn = "Ny journalpost på under-18-søker"
        override val beskrivelse = "Tar under-18-behandlingen av samtykkevent når en ny journalpost på søkeren er mottatt"
    }
}
