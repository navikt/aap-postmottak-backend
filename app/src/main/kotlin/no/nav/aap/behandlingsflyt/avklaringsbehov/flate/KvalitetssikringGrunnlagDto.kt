package no.nav.aap.behandlingsflyt.avklaringsbehov.flate

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.vedtak.TotrinnsVurdering

data class KvalitetssikringGrunnlagDto(val vurderinger: List<TotrinnsVurdering>, val historikk: List<Historikk>)
