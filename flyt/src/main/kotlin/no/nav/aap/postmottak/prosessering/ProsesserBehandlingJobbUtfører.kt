package no.nav.aap.postmottak.prosessering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.lås.TaSkriveLåsRepository

class ProsesserBehandlingJobbUtfører(
    private val låsRepository: TaSkriveLåsRepository,
    private val kontroller: FlytOrkestrator
) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val skrivelås = låsRepository.lås(BehandlingId(input.behandlingId()))

        val kontekst = kontroller.opprettKontekst(skrivelås.id)

        kontroller.forberedOgProsesserBehandling(kontekst)

        låsRepository.verifiserSkrivelås(skrivelås)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return ProsesserBehandlingJobbUtfører(
                repositoryProvider.provide(TaSkriveLåsRepository::class),
                FlytOrkestrator(repositoryProvider, gatewayProvider),
            )
        }

        override val type: String = "flyt.prosesserBehandling"

        override val navn: String = "Prosesser behandling"

        override val beskrivelse: String = "Ansvarlig for å drive prosessen på en gitt behandling"
    }
}
