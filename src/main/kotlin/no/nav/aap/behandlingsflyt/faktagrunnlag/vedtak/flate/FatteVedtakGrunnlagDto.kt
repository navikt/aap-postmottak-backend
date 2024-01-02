package no.nav.aap.behandlingsflyt.faktagrunnlag.vedtak.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering

data class FatteVedtakGrunnlagDto(val vurderinger: List<TotrinnsVurdering>)
