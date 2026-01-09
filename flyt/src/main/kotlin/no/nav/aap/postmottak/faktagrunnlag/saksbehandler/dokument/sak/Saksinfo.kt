package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak

import no.nav.aap.behandlingsflyt.kontrakt.statistikk.ResultatKode
import no.nav.aap.komponenter.type.Periode

data class Saksinfo(
    val saksnummer: String,
    val periode: Periode,
    val avslag: Boolean = false,
    val resultat: ResultatKode? = null,
)
