package no.nav.aap.postmottak.kontrakt.behandling

enum class TypeBehandling(private var identifikator: String) {

    DokumentHåndtering("ae1999");

    fun identifikator(): String = identifikator

    companion object {
        fun from(identifikator: String): TypeBehandling {
            return entries.first { it.identifikator == identifikator }
        }
    }
}

