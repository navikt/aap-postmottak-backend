package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

data class ArenaVideresenderKontekst(
    override val journalpostId: JournalpostId,
    override val innkommendeJournalpostId: Long,
    val ident: Ident,
    val hoveddokumenttittel: String,
    val vedleggstitler: List<String>,
    val navEnhet: String?
): ArenaBaseKontekst(journalpostId, innkommendeJournalpostId)

fun JobbInput.getArenaVideresenderKontekst() =
    DefaultJsonMapper.fromJson(this.payload(), ArenaVideresenderKontekst::class.java)

fun JobbInput.medArenaVideresenderKontekst(arenaVideresender: ArenaVideresenderKontekst): JobbInput {
    this.forSak(arenaVideresender.journalpostId.referanse)
    return this.medPayload(DefaultJsonMapper.toJson(arenaVideresender))
}

fun JournalpostMedDokumentTitler.opprettArenaVideresenderKontekst(enhet: String?, innkommendeJournalpostId: Long) =
    ArenaVideresenderKontekst(
        journalpostId = this.journalpostId,
        innkommendeJournalpostId = innkommendeJournalpostId,
        ident = this.person.aktivIdent(),
        navEnhet = enhet,
        hoveddokumenttittel = this.getHoveddokumenttittel(),
        vedleggstitler = this.getVedleggTitler()
    )
