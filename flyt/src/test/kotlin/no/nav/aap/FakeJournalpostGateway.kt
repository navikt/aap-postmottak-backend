package no.nav.aap

import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

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

}