package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId

interface AvklaringsbehovRepository: Repository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
}