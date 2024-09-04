package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.dokument.strukturering.flate

import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode

data class StruktureringVurderingDto(
    val vurdering: String?,
    val kategori: Brevkode,
    val dokumenter: List<Long>
)
