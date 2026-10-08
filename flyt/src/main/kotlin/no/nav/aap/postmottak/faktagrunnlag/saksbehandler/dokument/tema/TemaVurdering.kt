package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema

data class TemaVurdering(val skalTilAap: Boolean, val tema: Tema)

enum class Tema {
    AAP, OPP, UKJENT,
    EYB, BAR, BID, DAG, ENF, ERS, FEI, FOR, FUL, GEN, GRU, KOM, OMS, EYO,
    PEN, SAK, SER, SYK, TSO, TIL, IND, TRK, UFO, YRK;

    fun journalføresIPostmottak(): Boolean = this == AAP || this == OPP
    
    companion object {
        fun fraString(tema: String): Tema {
            return entries.firstOrNull { it.name == tema } ?: UKJENT
        }
    }
}