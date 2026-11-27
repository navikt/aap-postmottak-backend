package no.nav.aap.fordeler

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDateTime

interface InnkommendeJournalpostRepository: Repository {
    fun eksisterer(journalpostId: JournalpostId): Boolean
    fun hent(journalpostId: JournalpostId): InnkommendeJournalpost
    fun hentId(journalpostId: JournalpostId): Long
    fun hentHvisEksisterer(journalpostId: JournalpostId): InnkommendeJournalpost?
    fun finn(ident: Ident): List<InnkommendeJournalpost>
    fun harNyJournalpostEtter(identer: List<Ident>, tidspunkt: LocalDateTime, unntatt: JournalpostId): Boolean
    fun lagre(innkommendeJournalpost: InnkommendeJournalpost): Long
    fun update(innkommendeJournalpost: InnkommendeJournalpost)
}
