package no.nav.aap.postmottak.journalpostogbehandling.behandling

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface BehandlingRepository : Repository {

    fun opprettBehandling(journalpostId: JournalpostId, typeBehandling: TypeBehandling): BehandlingId

    fun hent(behandlingId: BehandlingId): Behandling

    fun hent(referanse: Behandlingsreferanse): Behandling

    fun hentAlleBehandlingerForSak(saksnummer: JournalpostId): List<Behandling>

    fun hentÅpenJournalføringsbehandling(journalpostId: JournalpostId): Behandling?

    fun hentBehandlingerForPerson(person: Person): List<Behandling>

    fun markerSavepoint()

    fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status)

    fun loggBesøktSteg(behandlingId: BehandlingId, tilstand: StegTilstand)

}

