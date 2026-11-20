package no.nav.aap.postmottak.repository.under18

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.repository.Factory
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import no.nav.aap.postmottak.under18.Under18Søknadsavklaring
import java.time.LocalDate

class Under18RepositoryImpl : Under18Repository {
    companion object : Factory<Under18RepositoryImpl> {
        override fun konstruer(connection: DBConnection): Under18RepositoryImpl = Under18RepositoryImpl()
    }

    override fun lagreSøknad(behandlingId: BehandlingId, journalpostId: JournalpostId) {
        TODO()
    }

    override fun hentSøknader(behandlingId: BehandlingId): List<JournalpostId> = TODO()

    override fun lagreFødselsdato(behandlingId: BehandlingId, fødselsdato: LocalDate) {
        TODO()
    }

    override fun hentFødselsdato(behandlingId: BehandlingId): LocalDate? = TODO()

    override fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring) {
        TODO()
    }

    override fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring? = TODO()

    override fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering) {
        TODO()
    }

    override fun hentSamtykkevurderinger(behandlingId: BehandlingId): List<Under18SamtykkeVurdering> = TODO()

    override fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering? = TODO()
}
