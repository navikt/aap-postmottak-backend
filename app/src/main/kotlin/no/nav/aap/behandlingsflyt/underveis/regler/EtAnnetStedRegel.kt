package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/**
 * Legger på perioder hvor bruker ikke har rett fordi hen er et annet sted
 *
 * - Utland
 * - Institusjon
 * - Straffegjennomføring
 *
 */
class EtAnnetStedRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering,Segment<Vurdering>>): Tidslinje<Vurdering,Segment<Vurdering>> {
        // TODO
        return resultat
    }
}