package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.finnsak.flate

import no.nav.aap.komponenter.type.Periode

data class SaksInfoDto(
    val saksnummer: String,
    val perioder: Periode
)

data class FinnSakVurderingDto(
    val saksnummer: String,
)

data class FinnSakGrunnlagDto(
    val vurdering: FinnSakVurderingDto?,
    val saksinfo: List<SaksInfoDto>
)
