package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.meldeplikt.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.meldeplikt.Fritaksvurdering

data class FritakMeldepliktGrunnlagDto(val vurderinger: List<Fritaksvurdering>)
