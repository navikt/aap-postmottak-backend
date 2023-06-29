package no.nav.aap.domene.behandling

import no.nav.aap.domene.typer.Periode

class Vilkårsperiode(
    private val periode: Periode,
    private val utfall: Utfall,
    private val faktagrunnlag: Faktagrunnlag,
    private val besluttningstre: Beslutningstre
) {

}
