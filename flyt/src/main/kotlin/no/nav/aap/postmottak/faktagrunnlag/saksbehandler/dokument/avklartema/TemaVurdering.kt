package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema

data class TemaVurdering(val skalTilAap: Boolean, val tema: Tema)

enum class Tema {
    AAP, OPP, UKJENT
}