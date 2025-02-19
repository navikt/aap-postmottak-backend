package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema

data class TemaVurdering(val skalTilAap: Boolean, val tema: Tema)

enum class Tema {
    AAP, OPP, UKJENT;
    
    companion object {
        fun fraString(tema: String): Tema {
            return when (tema) {
                "AAP" -> AAP
                "OPP" -> OPP
                else -> UKJENT
            }
        }
    }
}