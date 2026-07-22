package no.nav.aap.postmottak.forretningsflyt.steg.fordeling

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.arena.AapSystem
import no.nav.aap.fordeler.arena.ArenaVideresender
import no.nav.aap.fordeler.arena.AvklarFordelingRepository
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.flyt.steg.BehandlingSteg
import no.nav.aap.postmottak.flyt.steg.FlytSteg
import no.nav.aap.postmottak.flyt.steg.Fullført
import no.nav.aap.postmottak.flyt.steg.StegResultat
import no.nav.aap.postmottak.fordelingsCounter
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import org.slf4j.LoggerFactory


class FordelingVideresendSteg(
    val avklarFordelingRepository: AvklarFordelingRepository,
    val behandlingRepository: BehandlingRepository,
    val flytJobbRepository: FlytJobbRepository,
    val innkommendeJournalpostRepository: InnkommendeJournalpostRepository,
    val arenaVideresender: ArenaVideresender,
    val prometheus: MeterRegistry = SimpleMeterRegistry()
): BehandlingSteg {
    val logger = LoggerFactory.getLogger(FordelingVideresendSteg::class.java)

    companion object : FlytSteg {
        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider
        ): BehandlingSteg {
            return FordelingVideresendSteg(
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                ArenaVideresender.konstruer(repositoryProvider, gatewayProvider),
                PrometheusProvider.prometheus,
            )
        }

        override fun type(): StegType {
            return StegType.AVKLAR_FORDELING_VIDERESEND
        }
    }

    override fun utfør(kontekst: FlytKontekst): StegResultat {
        val vurdering = avklarFordelingRepository.hentVurderingHvisEksisterer(kontekst.behandlingId)

        vurdering?.system?.toFagsystem()?.let { prometheus.fordelingsCounter(it).increment() }

        when(vurdering?.system) {
            AapSystem.ARENA -> routeTilArena(kontekst.journalpostId)
            AapSystem.KELVIN -> routeTilKelvin(kontekst.journalpostId)
            AapSystem.BEGGE -> error(
                "Fordeling til både Arena og Kelvin er ikke støttet enda (behandling ${kontekst.behandlingId})"
            )
            else -> {
                logger.info("Journalpost med id ${kontekst.journalpostId} har status IGNORERT og blir ikke videresendt. Behandles av annet system.")
            }
        }

        return Fullført
    }

    private fun routeTilArena(journalpostId: JournalpostId) {
        logger.info("Oppretter IKKE behandling for journalpost som skal til Arena: $journalpostId")
        val inkommendeJournalpostId = innkommendeJournalpostRepository.hentId(journalpostId)
        arenaVideresender.videresendJournalpostTilArena(
            journalpostId,
            inkommendeJournalpostId
        )
    }

    private fun routeTilKelvin(journalpostId: JournalpostId) {
        logger.info("Oppretter behandling for journalpost som skal til Kelvin: $journalpostId")
        val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        flytJobbRepository.leggTil(
            JobbInput(ProsesserBehandlingJobbUtfører)
                .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
        )
    }
}
