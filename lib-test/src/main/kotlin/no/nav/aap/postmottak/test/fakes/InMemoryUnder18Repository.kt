package no.nav.aap.postmottak.test.fakes

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Aldersgrunnlag
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag

object InMemoryUnder18Repository : Under18Repository by IsolertUnder18Repository()

/**
 * Følger ikke transaksjoner. En registrert instans lever så lenge dens RepositoryProvider.
 */
class IsolertUnder18Repository : Under18Repository {
    companion object : Factory<IsolertUnder18Repository> {
        override fun konstruer(connection: DBConnection) = IsolertUnder18Repository()
    }

    private val søknadsgrunnlag = HashMap<BehandlingId, Under18Søknadsgrunnlag>()
    private val aldersgrunnlag = HashMap<BehandlingId, Under18Aldersgrunnlag>()
    private val søknadsavklaringer = HashMap<BehandlingId, Under18Søknadsavklaring>()
    private val samtykkevurderinger = HashMap<BehandlingId, Under18SamtykkeVurdering>()
    private val lock = Any()

    override fun hentBehandlingForSøknad(journalpostId: JournalpostId): BehandlingId? =
        synchronized(lock) { søknadsgrunnlag.entries.singleOrNull { journalpostId in it.value.søknader }?.key }

    override fun lagreSøknadsgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Søknadsgrunnlag) {
        synchronized(lock) {
            val tidligere = søknadsgrunnlag[behandlingId]
            if (tidligere == grunnlag) return
            tidligere?.validerEndringTil(grunnlag, behandlingId)
            grunnlag.søknader.forEach {
                val eier = hentBehandlingForSøknad(it)
                check(eier == null || eier == behandlingId) { "Søknad $it tilhører allerede en annen behandling" }
            }
            søknadsgrunnlag[behandlingId] = grunnlag.copy(søknader = grunnlag.søknader.toSet())
        }
    }

    override fun hentSøknadsgrunnlag(behandlingId: BehandlingId): Under18Søknadsgrunnlag =
        synchronized(lock) { søknadsgrunnlag.getValue(behandlingId).let { it.copy(søknader = it.søknader.toSet()) } }

    override fun lagreAldersgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Aldersgrunnlag) {
        synchronized(lock) { aldersgrunnlag[behandlingId] = grunnlag }
    }

    override fun hentAldersgrunnlag(behandlingId: BehandlingId): Under18Aldersgrunnlag? =
        synchronized(lock) { aldersgrunnlag[behandlingId] }

    override fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring) {
        synchronized(lock) { søknadsavklaringer[behandlingId] = avklaring }
    }

    override fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring? =
        synchronized(lock) { søknadsavklaringer[behandlingId] }

    override fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering) {
        synchronized(lock) { samtykkevurderinger[behandlingId] = vurdering }
    }

    override fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering? =
        synchronized(lock) { samtykkevurderinger[behandlingId] }
}
