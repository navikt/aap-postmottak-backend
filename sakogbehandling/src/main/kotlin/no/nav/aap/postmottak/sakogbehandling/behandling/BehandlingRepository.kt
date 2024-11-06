package no.nav.aap.postmottak.sakogbehandling.behandling

import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

interface BehandlingRepository {

    fun opprettBehandling(journalpostId: JournalpostId, typeBehandling: TypeBehandling): BehandlingId

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(referanse: Behandlingsreferanse): Behandling

    fun hentAlleBehandlingerForSak(saksnummer: JournalpostId): List<Behandling>

    fun hentÅpenJournalføringsbehandling(journalpostId: JournalpostId): Behandling
}

