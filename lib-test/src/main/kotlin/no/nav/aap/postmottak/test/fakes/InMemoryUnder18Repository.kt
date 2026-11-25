package no.nav.aap.postmottak.test.fakes

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Aldersgruppe
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import java.time.LocalDate

object InMemoryUnder18Repository : Under18Repository by IsolertUnder18Repository()

/**
 * Følger ikke transaksjoner. En registrert instans lever så lenge dens RepositoryProvider.
 */
class IsolertUnder18Repository : Under18Repository {
    companion object : Factory<IsolertUnder18Repository> {
        override fun konstruer(connection: DBConnection) = IsolertUnder18Repository()
    }

    private val søknader = HashMap<BehandlingId, MutableList<JournalpostId>>()
    private val fødselsdatoer = HashMap<BehandlingId, LocalDate>()
    private val aldersgrupper = HashMap<BehandlingId, Under18Aldersgruppe>()
    private val søknadsavklaringer = HashMap<BehandlingId, Under18Søknadsavklaring>()
    private val samtykkevurderinger = HashMap<BehandlingId, MutableList<Under18SamtykkeVurdering>>()
    private val lock = Any()
    private val under18Journalføringer = mutableSetOf<BehandlingId>()

    override fun registrerUnder18Journalføring(behandlingId: BehandlingId) {
        synchronized(lock) { under18Journalføringer.add(behandlingId) }
    }

    override fun erUnder18Journalføring(behandlingId: BehandlingId): Boolean =
        synchronized(lock) { behandlingId in under18Journalføringer }

    private val behandlingForNyeSøknaderPerPerson = mutableMapOf<Long, BehandlingId>()

    private val behandlingerStengtForNyeSøknader = mutableSetOf<BehandlingId>()

    override fun finnBehandlingForNyeSøknader(personId: Long): BehandlingId? = synchronized(lock) { behandlingForNyeSøknaderPerPerson[personId] }

    override fun registrerBehandlingForNyeSøknader(personId: Long, behandlingId: BehandlingId) {
        synchronized(lock) { check(behandlingForNyeSøknaderPerPerson.putIfAbsent(personId, behandlingId) == null) }
    }

    override fun stengForNyeSøknader(behandlingId: BehandlingId) {
        synchronized(lock) {
            behandlingForNyeSøknaderPerPerson.values.remove(behandlingId)
            behandlingerStengtForNyeSøknader.add(behandlingId)
        }
    }

    override fun hentBehandlingForSøknad(journalpostId: JournalpostId): BehandlingId? =
        synchronized(lock) { søknader.entries.singleOrNull { journalpostId in it.value }?.key }

    override fun lagreSøknad(behandlingId: BehandlingId, journalpostId: JournalpostId) {
        synchronized(lock) {
            check(behandlingId !in behandlingerStengtForNyeSøknader) { "Under18-behandling $behandlingId tar ikke imot nye søknader." }
            val liste = søknader.getOrPut(behandlingId) { mutableListOf() }
            require(hentBehandlingForSøknad(journalpostId) == null) { "Søknad $journalpostId er allerede lagret" }
            liste.add(journalpostId)
        }
    }

    override fun hentSøknader(behandlingId: BehandlingId): List<JournalpostId> =
        synchronized(lock) { søknader[behandlingId].orEmpty().toList() }

    override fun lagreFødselsdato(behandlingId: BehandlingId, fødselsdato: LocalDate) {
        synchronized(lock) { fødselsdatoer[behandlingId] = fødselsdato }
    }

    override fun hentFødselsdato(behandlingId: BehandlingId): LocalDate? =
        synchronized(lock) { fødselsdatoer[behandlingId] }

    override fun lagreAldersgruppe(behandlingId: BehandlingId, aldersgruppe: Under18Aldersgruppe) {
        synchronized(lock) { aldersgrupper[behandlingId] = aldersgruppe }
    }

    override fun hentAldersgruppe(behandlingId: BehandlingId): Under18Aldersgruppe? =
        synchronized(lock) { aldersgrupper[behandlingId] }

    override fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring) {
        synchronized(lock) { søknadsavklaringer[behandlingId] = avklaring }
    }

    override fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring? =
        synchronized(lock) { søknadsavklaringer[behandlingId] }

    override fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering) {
        synchronized(lock) { samtykkevurderinger.getOrPut(behandlingId) { mutableListOf() }.add(vurdering) }
    }

    override fun hentSamtykkevurderinger(behandlingId: BehandlingId): List<Under18SamtykkeVurdering> =
        synchronized(lock) { samtykkevurderinger[behandlingId].orEmpty().toList() }

    override fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering? =
        synchronized(lock) { samtykkevurderinger[behandlingId]?.lastOrNull() }
}
