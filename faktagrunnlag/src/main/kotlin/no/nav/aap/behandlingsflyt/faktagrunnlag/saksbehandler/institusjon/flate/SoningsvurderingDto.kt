package no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.flate

import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.Soningsvurdering
import no.nav.aap.verdityper.dokument.JournalpostId

data class SoningsvurderingDto(
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val soningUtenforFengsel: Boolean,
    val begrunnelse: String?,
    val arbeidUtenforAnstalt: Boolean? = null
) {

    fun tilDomeneobjekt() = Soningsvurdering(
        dokumenterBruktIVurdering = listOf(),
        begrunnelse = begrunnelse,
        arbeidUtenforAnstalt = arbeidUtenforAnstalt,
        soningUtenforFengsel = soningUtenforFengsel,
    )

    companion object {
        fun toDto(soningsvurdering: Soningsvurdering?) = if (soningsvurdering != null) SoningsvurderingDto(
            dokumenterBruktIVurdering = soningsvurdering.dokumenterBruktIVurdering,
            arbeidUtenforAnstalt = soningsvurdering.arbeidUtenforAnstalt,
            begrunnelse = soningsvurdering.begrunnelse,
            soningUtenforFengsel = soningsvurdering.soningUtenforFengsel,
        ) else null
    }
}
