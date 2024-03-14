package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje

/**
 * Håndterer varighetsbestemmelsene (11-12 + unntak fra denne). Sjekker uttak mot kvoten etablert i saken.
 *
 * - Varigheten på ordinær (3 år)
 * - Unntak
 *   - Utvidelse (2 år)
 *   - Sykepengererstatning (6 måneder)
 *   - Venter på uføre (4 + 4 måneder)
 *   - Avklart arbeid (??)
 * - Dødsfall på bruker
 *
 */
class VarighetRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering, Segment<Vurdering>>): Tidslinje<Vurdering, Segment<Vurdering>> {
        // TODO
        return resultat
    }
}