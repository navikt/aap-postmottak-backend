package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface BehandlingRepository {

    fun opprettBehandling(journalpostId: JournalpostId): Behandling

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(journalpostId: JournalpostId): Behandling
}

