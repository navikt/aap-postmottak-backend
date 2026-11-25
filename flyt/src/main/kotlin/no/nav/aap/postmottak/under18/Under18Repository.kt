package no.nav.aap.postmottak.under18

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate

interface Under18Repository : Repository {
    fun registrerUnder18Journalføring(behandlingId: BehandlingId)
    fun erUnder18Journalføring(behandlingId: BehandlingId): Boolean
    fun finnBehandlingForNyeSøknader(personId: Long): BehandlingId?
    fun hentBehandlingForSøknad(journalpostId: JournalpostId): BehandlingId?
    fun lagreSøknadsgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Søknadsgrunnlag)
    fun hentSøknadsgrunnlag(behandlingId: BehandlingId): Under18Søknadsgrunnlag

    fun lagreAldersgrunnlag(behandlingId: BehandlingId, grunnlag: Under18Aldersgrunnlag)
    fun hentAldersgrunnlag(behandlingId: BehandlingId): Under18Aldersgrunnlag?

    fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring)
    fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring?

    fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering)
    fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering?
}

data class Under18Aldersgrunnlag(val fødselsdato: LocalDate, val aldersgruppe: Under18Aldersgruppe)

fun Under18Repository.hentFødselsdatoFor(behandlingId: BehandlingId): LocalDate =
    checkNotNull(hentAldersgrunnlag(behandlingId)) { "Fødselsdato er ikke lagret for behandling $behandlingId" }.fødselsdato
