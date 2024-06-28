package no.nav.aap.behandlingsflyt.behandling.underveis.regler

import no.nav.aap.tidslinje.Tidslinje

interface UnderveisRegel {

    fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering>
}