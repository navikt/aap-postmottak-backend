package no.nav.aap.postmottak.under18

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDateTime

interface Under18Repository : Repository {
    fun lagreSøknad(behandlingId: BehandlingId, journalpostId: JournalpostId)
    fun hentSøknader(behandlingId: BehandlingId): List<JournalpostId>

    fun lagreSamtykkevurdering(behandlingId: BehandlingId, vurdering: Under18SamtykkeVurdering)
    fun hentSamtykkevurderinger(behandlingId: BehandlingId): List<Under18SamtykkeVurdering>
    fun hentAktivSamtykkevurdering(behandlingId: BehandlingId): Under18SamtykkeVurdering?
    fun lagreVentOpprettetTid(behandlingId: BehandlingId, tidspunkt: LocalDateTime)
}
