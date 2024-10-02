package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface BehandlingRepository {

    fun opprettBehandling(journalpostId: JournalpostId): BehandlingId

    fun hentMedLås(behandlingId: BehandlingId, versjon: Long? = null): Behandling

    fun hentMedLås(journalpostId: JournalpostId, versjon: Long? = null): Behandling

    fun hent(journalpostId: JournalpostId, versjon: Long? = null): Behandling
}

