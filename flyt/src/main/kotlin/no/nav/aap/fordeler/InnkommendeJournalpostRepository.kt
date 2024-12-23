package no.nav.aap.fordeler

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

interface InnkommendeJournalpostRepository: Repository {
    fun hent(journalpostId: JournalpostId): InnkommendeJournalpost
    fun lagre(innkommendeJournalpost: InnkommendeJournalpost)
}
