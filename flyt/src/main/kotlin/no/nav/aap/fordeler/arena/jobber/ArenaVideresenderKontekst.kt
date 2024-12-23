package no.nav.aap.fordeler.arena.jobber

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.JournalpostMedDokumentTitler
import no.nav.aap.postmottak.journalpostogbehandling.Ident

data class ArenaVideresenderKontekst(
    val journalpostId: JournalpostId,
    val ident: Ident,
    val hoveddokumenttittel: String,
    val vedleggstitler: List<String>,
    val navEnhet: String
)

fun JobbInput.getArenaVideresenderKontekst() =
    DefaultJsonMapper.fromJson(this.payload(), ArenaVideresenderKontekst::class.java)

fun JobbInput.medArenaVideresenderKontekst(arenaVideresender: ArenaVideresenderKontekst): JobbInput {
    this.forSak(arenaVideresender.journalpostId.referanse)
    return this.medPayload(DefaultJsonMapper.toJson(arenaVideresender))
}

fun JournalpostMedDokumentTitler.opprettArenaVideresenderKontekst(enhet: String) =
    ArenaVideresenderKontekst(
        journalpostId = this.journalpostId,
        ident = this.person.aktivIdent(),
        navEnhet = enhet,
        hoveddokumenttittel = this.getHoveddokumenttittel(),
        vedleggstitler = this.getVedleggTitler()
    )
