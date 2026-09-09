package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.type.Periode

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode,
    val avslag: Boolean = false,
    val resultat: ResultatKode? = null,
    val finnesÅpenBehandling: Boolean? = null,
)

/**
 * Legeerklæringer skal ikke trigge automatisk revurdering dersom det er avslag på siste AAP-søknad,
 * med mindre det finnes en åpen behandling.
 */
fun List<Saksinfo>.tillaterAutomatiskBehandlingAvLegeerklæring(): Boolean {
    val harÅpenBehandling = any { it.finnesÅpenBehandling == true }
    val harIkkeAvslag = any { !it.avslag }
    return isEmpty() || harÅpenBehandling || harIkkeAvslag
}

