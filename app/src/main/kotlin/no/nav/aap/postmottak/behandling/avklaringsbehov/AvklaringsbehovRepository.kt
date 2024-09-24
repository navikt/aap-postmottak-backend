package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface AvklaringsbehovRepository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
}