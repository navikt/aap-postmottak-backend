package no.nav.aap.postmottak.sakogbehandling.behandling.vurdering

import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode


data class TemaVurdeirng(val avklaring: Boolean)

data class KategoriVurdering(val avklaring: Brevkode)

data class Saksvurdering(
    val saksnummer: String? = null,
    val opprettNySak: Boolean = false,
    val generellSak: Boolean = false,
) {
    init {
        require(saksnummer != null || opprettNySak || generellSak) { "Sak må oppgis"}
    }
}

data class Struktureringsvurdering(val vurdering: String)

class Vurderinger(
    val avklarTemaVurdering: TemaVurdeirng? = null,
    val kategorivurdering: KategoriVurdering? = null,
    val struktureringsvurdering: Struktureringsvurdering? = null,
    val saksvurdering: Saksvurdering?  = null
)
