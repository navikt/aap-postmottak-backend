package no.nav.aap.postmottak.test.inmemoryrepo

import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.StegTilstand
import java.util.concurrent.ConcurrentHashMap

object InMemoryBehandlingRepository : BehandlingRepository {
    private val behandlinger = ConcurrentHashMap<BehandlingId, Behandling>()

    fun lagre(behandling: Behandling) {
        behandlinger[behandling.id] = behandling
    }

    override fun opprettBehandling(
        journalpostId: JournalpostId,
        typeBehandling: TypeBehandling,
    ): BehandlingId = error("Ikke implementert i test-repositoriet")

    override fun hent(behandlingId: BehandlingId) =
        checkNotNull(behandlinger[behandlingId]) { "Fant ikke behandling $behandlingId" }

    override fun hent(referanse: Behandlingsreferanse) =
        behandlinger.values.single { it.referanse.referanse == referanse.referanse }

    override fun hentSisteBehandling(journalpostId: JournalpostId) =
        behandlinger.values.filter { it.journalpostId == journalpostId }.maxOrNull()
            ?: error("Fant ikke behandling for journalpost $journalpostId")

    override fun hentAlleBehandlingerForJournalpost(journalpostId: JournalpostId) =
        behandlinger.values.filter { it.journalpostId == journalpostId }

    override fun hentÅpenJournalføringsbehandling(journalpostId: JournalpostId): Behandling? = null

    override fun hentÅpenUnder18Behandling(personId: Long, forOppdatering: Boolean): BehandlingId? =
        error("Ikke implementert i test-repositoriet")

    override fun hentBehandlingerForPerson(person: Person): List<Behandling> = emptyList()

    override fun markerSavepoint() = Unit

    override fun oppdaterBehandlingStatus(behandlingId: BehandlingId, status: Status) = Unit

    override fun loggBesøktSteg(
        behandlingId: BehandlingId,
        tilstand: StegTilstand,
    ) = Unit
}
