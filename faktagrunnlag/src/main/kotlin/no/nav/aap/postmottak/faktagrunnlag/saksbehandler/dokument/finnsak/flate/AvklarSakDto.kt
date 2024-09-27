package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.flate

import no.nav.aap.komponenter.type.Periode

data class SaksInfoDto(
    val saksnummer: String,
    val periode: Periode
)

data class AvklarSakVurderingDto(
    val saksnummer: String,
)

data class AvklarSakGrunnlagDto(
    val vurdering: AvklarSakVurderingDto?,
    val saksinfo: List<SaksInfoDto>
)
