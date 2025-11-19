package no.nav.aap.postmottak.kontrakt.behandling

public enum class TypeBehandling(private var identifikator: String) {

    DokumentHåndtering("ae1999"),
    Journalføring("ae2000");

    public fun identifikator(): String = identifikator

    public companion object {
        public fun from(identifikator: String): TypeBehandling {
            return entries.first { it.identifikator == identifikator }
        }
    }
}

