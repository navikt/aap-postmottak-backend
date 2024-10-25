package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.motor.JobbInput
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

fun JobbInput.forBehandling(behandlingId: BehandlingId) = this.forBehandling(1, behandlingId.id)