package no.nav.aap.postmottak.under18

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDate

interface Under18Repository : Repository {
    fun finnBehandlingForNyeSøknader(personId: Long): BehandlingId?
    fun registrerBehandlingForNyeSøknader(personId: Long, behandlingId: BehandlingId)
    fun stengForNyeSøknader(behandlingId: BehandlingId)
    fun hentBehandlingForSøknad(journalpostId: JournalpostId): BehandlingId?
    fun lagreSøknad(behandlingId: BehandlingId, journalpostId: JournalpostId)
    fun hentSøknader(behandlingId: BehandlingId): List<JournalpostId>

    fun lagreFødselsdato(behandlingId: BehandlingId, fødselsdato: LocalDate)
    fun hentFødselsdato(behandlingId: BehandlingId): LocalDate?
    fun lagreAldersgruppe(behandlingId: BehandlingId, aldersgruppe: Under18Aldersgruppe)
    fun hentAldersgruppe(behandlingId: BehandlingId): Under18Aldersgruppe?

    fun lagreSøknadsavklaring(behandlingId: BehandlingId, avklaring: Under18Søknadsavklaring)
    fun hentSøknadsavklaring(behandlingId: BehandlingId): Under18Søknadsavklaring?

    fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering)
    fun hentSamtykkevurderinger(behandlingId: BehandlingId): List<Under18SamtykkeVurdering>
    fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering?
}

fun Under18Repository.hentFødselsdatoFor(behandlingId: BehandlingId): LocalDate =
    checkNotNull(hentFødselsdato(behandlingId)) { "Fødselsdato er ikke lagret for behandling $behandlingId" }
