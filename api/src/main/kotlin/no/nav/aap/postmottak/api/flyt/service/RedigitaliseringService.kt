package no.nav.aap.postmottak.api.flyt.service

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.prosessering.medSaksnummer
import no.nav.aap.postmottak.redigitalisering.RedigitaliseringKopierJobbUtfører
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.slf4j.LoggerFactory

class RedigitaliseringService(
    private val flytJobbRepository: FlytJobbRepository,
    private val behandlingRepository: BehandlingRepository,
    private val journalpostRepository: JournalpostRepository,
    private val saksnummerRepository: SaksnummerRepository,
    private val unleashGateway: UnleashGateway
) {
    val log = LoggerFactory.getLogger(RedigitaliseringService::class.java)

    companion object {
        fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): RedigitaliseringService {
            return RedigitaliseringService(
                flytJobbRepository = repositoryProvider.provide(),
                behandlingRepository = repositoryProvider.provide(),
                journalpostRepository = repositoryProvider.provide(),
                saksnummerRepository = repositoryProvider.provide(),
                unleashGateway = gatewayProvider.provide<UnleashGateway>()
            )
        }
    }

    fun redigitaliser(journalpostId: Long, saksnummer: String): String? {
        val journalpost = journalpostRepository.hentHvisEksisterer(JournalpostId(journalpostId))
        if (journalpost?.redigitalisert == true) {
            return "Journalpost har allerede blitt redigitalisert."
        }

        if (journalpost?.journalpostId == null && !unleashGateway.isEnabled(PostmottakFeature.RedigitaliseringV2)) {
            // bevisst tryn som før
            requireNotNull(journalpost)
        }

        if (journalpost != null) {
            val behandling = behandlingRepository.hent(journalpost.journalpostId)
            requireNotNull(behandling) { "Behandling ikke funnet. Req: ${journalpost.journalpostId.referanse}." }

            val eksisterendeSak = saksnummerRepository.hentSakVurdering(behandling.id)
            requireNotNull(eksisterendeSak) { "Det må eksistere en sak fra før. Req: ${saksnummer}." }
            require(eksisterendeSak.saksnummer == saksnummer) { "Saksnummer innsendt avviker fra registrert saksnummer i postmottak. Req: ${saksnummer}." }
        }

        log.info("Legger til kopieringsjobb for redigitalisering av journalpost $journalpostId")
        flytJobbRepository.leggTil(
            JobbInput(RedigitaliseringKopierJobbUtfører)
                .forSak(journalpostId)
                .medJournalpostId(JournalpostId(journalpostId))
                .medSaksnummer(saksnummer)
                .medCallId()
        )
        return null
    }
}