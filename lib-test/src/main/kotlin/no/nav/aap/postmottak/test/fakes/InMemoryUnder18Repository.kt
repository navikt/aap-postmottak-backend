package no.nav.aap.postmottak.test.fakes

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import java.time.LocalDate

/**
 * Følger ikke transaksjoner: data som er lagret før en rollback, blir liggende. Tilstanden er global,
 * så testene må bruke unike behandlings-ID-er.
 */
object InMemoryUnder18Repository : Under18Repository {
    private val søknader = HashMap<BehandlingId, MutableList<JournalpostId>>()
    private val fødselsdatoer = HashMap<BehandlingId, LocalDate>()
    private val søknadsavklaringer = HashMap<BehandlingId, Under18Søknadsavklaring>()
    private val samtykkevurderinger = HashMap<BehandlingId, MutableList<Under18SamtykkeVurdering>>()
    private val lock = Any()

    override fun lagreSøknad(behandlingId: BehandlingId, journalpostId: JournalpostId) {
        synchronized(lock) {
            val liste = søknader.getOrPut(behandlingId) { mutableListOf() }
            require(journalpostId !in liste) { "Søknad $journalpostId er allerede lagret på behandling $behandlingId" }
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
