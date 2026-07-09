package no.nav.aap.postmottak.api.faktagrunnlag.sak

import no.nav.aap.postmottak.gateway.ArenaSakskontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.VurderOpprettelseAvSakValg

/**
 * Grunnlag som vises til saksbehandler i det manuelle avklaringsbehovet VURDER_OPPRETTELSE_AV_SAK.
 * Alt er nullbart: [arenaSakskontekst] hentes ennå ikke (returnerer null), og [valg]/[begrunnelse]
 * er kun satt dersom behovet allerede er (delvis) løst.
 */
data class VurderOpprettelseAvSakGrunnlagDto(
    val valg: VurderOpprettelseAvSakValg? = null,
    val begrunnelse: String? = null,
    val arenaSakskontekst: ArenaSakskontekst? = null,
)

