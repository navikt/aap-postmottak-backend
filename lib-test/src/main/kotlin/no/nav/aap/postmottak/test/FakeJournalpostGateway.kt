package no.nav.aap.postmottak.test

import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.gateway.SafTilknyttetJournalpost
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.TestJournalposter

class FakeJournalpostGateway : JournalpostGateway {
    companion object : Factory<FakeJournalpostGateway> {
        override fun konstruer(): FakeJournalpostGateway {
            return FakeJournalpostGateway()
        }
    }

    override fun hentJournalpost(journalpostId: JournalpostId): SafJournalpost {
        TODO("Not yet implemented")
    }

    override fun hentSaker(fnr: String): List<SafSak> {
        return emptyList()
    }

    override fun hentTilknyttedeJournalposter(dokumentInfoId: DokumentInfoId): List<SafTilknyttetJournalpost> =
        TestJournalposter.hentTilknyttedeJournalposter(dokumentInfoId)

}