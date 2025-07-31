package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class ArenaVideresenderKontekst(
    override val journalpostId: JournalpostId,
    override val innkommendeJournalpostId: Long,
    val ident: Ident,
    val hoveddokumenttittel: String,
    val vedleggstitler: List<String>,
    val navEnhet: String?
) : ArenaBaseKontekst(journalpostId, innkommendeJournalpostId) {
    companion object {
        fun fra(journalpost: Journalpost, enhet: String?, innkommendeJournalpostId: Long) = ArenaVideresenderKontekst(
            journalpostId = journalpost.journalpostId,
            innkommendeJournalpostId = innkommendeJournalpostId,
            ident = journalpost.person.aktivIdent(),
            navEnhet = enhet,
            hoveddokumenttittel = journalpost.getHoveddokumenttittel(),
            vedleggstitler = journalpost.getVedleggTitler()
        )
    }
}

fun JobbInput.getArenaVideresenderKontekst() =
    DefaultJsonMapper.fromJson(this.payload(), ArenaVideresenderKontekst::class.java)

fun JobbInput.medArenaVideresenderKontekst(arenaVideresender: ArenaVideresenderKontekst): JobbInput {
    this.forSak(arenaVideresender.journalpostId.referanse)
    return this.medPayload(DefaultJsonMapper.toJson(arenaVideresender))
}
