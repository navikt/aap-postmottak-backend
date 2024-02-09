package no.nav.aap.behandlingsflyt.tilkjentytelse

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.tidslinje.Segment
import no.nav.aap.tidslinje.Tidslinje
import no.nav.aap.verdityper.GUnit
import no.nav.aap.verdityper.Periode
import java.time.LocalDate

interface MinsteÅrligYtelseStrategi {
    fun aldersjustertMinsteÅrligYtelse(minsteÅrligYtelseVerdi: GUnit): GUnit
}

object under25 : MinsteÅrligYtelseStrategi {
    override fun aldersjustertMinsteÅrligYtelse(misteÅrligYtelseVerdi: GUnit): GUnit {
        return misteÅrligYtelseVerdi.multiplisert(2).dividert(3)
    }
}

object over25 : MinsteÅrligYtelseStrategi {
    override fun aldersjustertMinsteÅrligYtelse(misteÅrligYtelseVerdi: GUnit): GUnit {
        return misteÅrligYtelseVerdi
    }
}

class MinsteÅrligYtelseAlderTidslinje(val fødselsdato: Fødselsdato) {
    fun tilTidslinje(): Tidslinje<MinsteÅrligYtelseStrategi> {
        return Tidslinje(
            listOf(
                Segment(
                    periode = Periode(LocalDate.MIN, fødselsdato.`25årsDagen`().minusDays(1)),
                    verdi = under25
                ),
                Segment(
                    periode = Periode(fødselsdato.`25årsDagen`(), LocalDate.MAX),
                    verdi = over25
                )
            )
        )
    }

}