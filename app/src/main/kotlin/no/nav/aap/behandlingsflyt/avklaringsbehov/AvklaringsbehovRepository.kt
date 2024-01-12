package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface AvklaringsbehovRepository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
}