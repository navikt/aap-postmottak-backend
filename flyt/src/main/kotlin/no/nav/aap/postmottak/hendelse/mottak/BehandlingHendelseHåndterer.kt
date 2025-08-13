package no.nav.aap.postmottak.hendelse.mottak

import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovOrkestrator
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

class BehandlingHendelseHåndterer(repositoryProvider: RepositoryProvider) {

    private val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
        repositoryProvider
    )

    fun håndtere(key: BehandlingId, hendelse: BehandlingHendelse) {
        when (hendelse) {
            is BehandlingSattPåVent -> {
                avklaringsbehovOrkestrator.settBehandlingPåVent(key, hendelse)
            }

        }
    }
}