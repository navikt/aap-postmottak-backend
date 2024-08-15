package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.flate

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandGrunnerSerializer
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.bistand.BistandVurdering

data class BistandGrunnlagDto(
    @JsonProperty("vurdering")
    @JsonSerialize(using = BistandGrunnerSerializer::class)
    val vurdering: BistandVurdering?
)
