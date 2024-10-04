package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.Saksvurdering

data class SaksInfoDto(
    val saksnummer: String,
    val periode: Periode
)

data class AvklarSakVurderingDto(
    val saksnummer: String?,
    val opprettNySak: Boolean,
    val førPåGenerellSak: Boolean,
) {
    companion object {
        fun toDto(saksvurdering: Saksvurdering) = AvklarSakVurderingDto(
            saksvurdering.saksnummer, saksvurdering.opprettNySak, saksvurdering.opprettNySak
        )
    }
}

data class AvklarSakGrunnlagDto(
    val vurdering: AvklarSakVurderingDto?,
    val saksinfo: List<SaksInfoDto>
)
