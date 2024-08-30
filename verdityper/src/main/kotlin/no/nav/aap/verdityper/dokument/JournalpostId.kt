package no.nav.aap.verdityper.dokument

data class JournalpostId(val identifikator: Long) {
    override fun toString(): String {
        return "Journalpost[id=$identifikator]"
    }
}
