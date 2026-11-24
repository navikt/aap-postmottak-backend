package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.Instant

interface InformasjonskravRepository : Repository {
    fun hentOppdateringer(
        journalpostId: JournalpostId,
        informasjonskrav: List<InformasjonskravNavn>,
    ): List<InformasjonskravOppdatert>

    fun registrerOppdateringer(
        journalpostId: JournalpostId,
        behandlingId: BehandlingId,
        informasjonskrav: Map<InformasjonskravNavn, InformasjonskravInput>,
        oppdatert: Instant,
    )
}
