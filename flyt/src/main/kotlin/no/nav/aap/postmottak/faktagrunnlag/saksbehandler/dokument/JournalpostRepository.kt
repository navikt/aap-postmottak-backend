package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface JournalpostRepository: Repository {
    fun hentHvisEksisterer(behandlingId: BehandlingId): Journalpost?
    fun hentHvisEksisterer(behandlingsreferanse: Behandlingsreferanse): Journalpost?
    fun hentHvisEksisterer(journalpostId: JournalpostId): Journalpost?
    fun lagre(journalpost: Journalpost)
}