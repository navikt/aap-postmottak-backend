package no.nav.aap.behandlingsflyt.avklaringsbehov.bistand

data class BistandsVurdering(
    val begrunnelse: String,
    val erBehovForBistand: Boolean
)