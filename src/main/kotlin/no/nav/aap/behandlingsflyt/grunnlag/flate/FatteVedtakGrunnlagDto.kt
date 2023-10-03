package no.nav.aap.behandlingsflyt.grunnlag.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.vedtak.TotrinnsVurdering

data class FatteVedtakGrunnlagDto(val vurderinger: List<TotrinnsVurdering>)
