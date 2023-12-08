package no.nav.aap.behandlingsflyt.underveis.regler

import no.nav.aap.behandlingsflyt.beregning.Prosent
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Segment
import no.nav.aap.behandlingsflyt.underveis.tidslinje.Tidslinje

/**
 * Graderer arbeid der hvor det ikke er avslått pga en regel tidliger i løpet
 *
 * - Arbeid fra meldeplikt
 */
class GraderingArbeidRegel : UnderveisRegel {
    override fun vurder(input: UnderveisInput, resultat: Tidslinje<Vurdering>): Tidslinje<Vurdering> {
        // TODO: Legge inn noe reelt
        val arbeidsTidslinje = Tidslinje(resultat.perioder().map { Segment(it, Prosent(100)) })

        return resultat.kombiner(arbeidsTidslinje, LeggTilGraderingPåVurderingerSammenslåer())
    }
}