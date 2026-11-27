package no.nav.aap.postmottak.test.inmemoryrepo

import no.nav.aap.fordeler.InnkommendeJournalpost
import no.nav.aap.fordeler.InnkommendeJournalpostRepository
import no.nav.aap.fordeler.Mottakshendelse
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import java.time.LocalDateTime
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object InMemoryInnkommendeJournalpostRepository : InnkommendeJournalpostRepository {
    private data class Mottak(
        val journalpost: InnkommendeJournalpost,
        val id: Long,
        val mottattTid: LocalDateTime,
    )

    private val mottak = ConcurrentHashMap<JournalpostId, Mottak>()
    private val ids = AtomicLong()

    override fun eksisterer(journalpostId: JournalpostId) = mottak.containsKey(journalpostId)

    override fun hent(journalpostId: JournalpostId) =
        checkNotNull(mottak[journalpostId]) { "Fant ikke innkommende journalpost $journalpostId" }.journalpost

    override fun hentId(journalpostId: JournalpostId) =
        checkNotNull(mottak[journalpostId]) { "Fant ikke innkommende journalpost $journalpostId" }.id

    override fun hentHvisEksisterer(journalpostId: JournalpostId) = mottak[journalpostId]?.journalpost

    override fun finn(ident: Ident) =
        mottak.values.filter { it.journalpost.brukerId == ident.identifikator }.map { it.journalpost }

    override fun finnMottakshistorikk(identer: List<Ident>): List<Mottakshendelse> {
        val identifikatorer = identer.mapTo(HashSet()) { it.identifikator }
        return mottak.values
            .filter { it.journalpost.brukerId in identifikatorer }
            .map { Mottakshendelse(it.journalpost.journalpostId, it.mottattTid) }
            .sortedBy { it.mottattTid }
    }

    override fun lagre(innkommendeJournalpost: InnkommendeJournalpost): Long {
        val id = ids.incrementAndGet()
        mottak[innkommendeJournalpost.journalpostId] = Mottak(
            journalpost = innkommendeJournalpost,
            id = id,
            mottattTid = LocalDateTime.now().plusSeconds(1),
        )
        return id
    }

    override fun update(innkommendeJournalpost: InnkommendeJournalpost) {
        val forrige = mottak[innkommendeJournalpost.journalpostId]
        mottak[innkommendeJournalpost.journalpostId] = Mottak(
            journalpost = innkommendeJournalpost,
            id = forrige?.id ?: ids.incrementAndGet(),
            mottattTid = forrige?.mottattTid ?: LocalDateTime.now().plusSeconds(1),
        )
    }
}
