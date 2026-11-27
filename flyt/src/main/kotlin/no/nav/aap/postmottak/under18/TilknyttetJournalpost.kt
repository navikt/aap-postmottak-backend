package no.nav.aap.postmottak.under18

import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.SafTilknyttetJournalpost
import no.nav.aap.postmottak.gateway.Sakstype
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

fun List<SafTilknyttetJournalpost>.finnPåKelvinSak(
    kilde: JournalpostId, dokumentIder: Set<String>, saksnummer: String
): JournalpostId? {
    val kandidater = filterNot { it.journalpostId.toLong() == kilde.referanse }.filter {
        val sak = checkNotNull(it.sak) { "SAF mangler sak for tilknyttet journalpost ${it.journalpostId}" }
        sak.sakstype == Sakstype.FAGSAK && sak.fagsaksystem == Fagsystem.KELVIN.name && sak.fagsakId == saksnummer
    }
    check(kandidater.all { it.dokumenter.map { dok -> dok.dokumentInfoId }.toSet() == dokumentIder }) {
        "Uventet dokumentsett på Kelvin-saken for $kilde"
    }
    check(kandidater.size <= 1) { "Flere tilknytninger til Kelvin-saken for $kilde" }
    return kandidater.singleOrNull()?.let { JournalpostId(it.journalpostId.toLong()) }
}
