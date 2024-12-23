package no.nav.aap.postmottak.journalpostogbehandling.lås

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse

interface TaSkriveLåsRepository: Repository {
    fun lås( behandlingId: BehandlingId): BehandlingSkrivelås

    fun lås(referanse: Behandlingsreferanse): BehandlingSkrivelås

    fun verifiserSkrivelås(skrivelås: BehandlingSkrivelås)
}
