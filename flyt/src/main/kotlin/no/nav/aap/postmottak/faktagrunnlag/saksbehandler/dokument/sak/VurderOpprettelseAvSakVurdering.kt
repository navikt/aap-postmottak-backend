package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg

/**
 * Saksbehandlers vurdering fra det manuelle avklaringsbehovet
 * [no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon.VURDER_OPPRETTELSE_AV_SAK].
 *
 * Alt er nullbart med vilje – vurderingen kan være delvis utfylt, og [valg] settes først når
 * saksbehandler faktisk løser behovet.
 */
data class VurderOpprettelseAvSakVurdering(
    val valg: VurderOpprettelseAvSakValg? = null,
    val begrunnelse: String? = null,
)

