package no.nav.aap.postmottak.api.faktagrunnlag.sak

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering

data class SaksInfoDto(
    val saksnummer: String,
    val periode: Periode
)

data class AvklarSakVurderingDto(
    val saksnummer: String?,
    val førPåGenerellSak: Boolean,
) {
    companion object {
        fun toDto(saksvurdering: Saksvurdering) = AvklarSakVurderingDto(
            saksvurdering.saksnummer, saksvurdering.generellSak
        )
    }
}

data class AvklarSakGrunnlagDto(
    val vurdering: AvklarSakVurderingDto?,
    val saksinfo: List<SaksInfoDto>
)
