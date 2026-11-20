package no.nav.aap.postmottak.under18

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehov
import no.nav.aap.postmottak.flyt.ventebehov.SpesifikkVentebehovEvaluerer
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status

class Under18SamtykkeVentebehovEvaluerer(
    private val repositoryProvider: RepositoryProvider,
) : SpesifikkVentebehovEvaluerer {
    private val behandlingRepository: BehandlingRepository by lazy { repositoryProvider.provide() }
    private val samtykkekandidater: Under18Samtykkekandidater by lazy {
        Under18Samtykkekandidater.konstruer(repositoryProvider)
    }

    override fun definisjon() = Definisjon.VENT_PA_UNDER18_SAMTYKKE

    override fun ansesSomLøst(kontekst: FlytKontekst, avklaringsbehov: Avklaringsbehov): Boolean {
        val ventSatt = avklaringsbehov.historikk
            .filter { it.status == Status.OPPRETTET }
            .maxOf { it.tidsstempel }

        return samtykkekandidater.hent(behandlingRepository.hent(kontekst.behandlingId))
            .any { it.mottattTid > ventSatt }
    }
}
