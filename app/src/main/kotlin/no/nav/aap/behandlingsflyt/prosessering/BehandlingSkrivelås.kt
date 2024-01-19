package no.nav.aap.behandlingsflyt.prosessering

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.TypeBehandling

data class BehandlingSkrivelås(val id: BehandlingId, val versjon: Long, val typeBehandling: TypeBehandling)
