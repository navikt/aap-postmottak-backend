package no.nav.aap.postmottak.test.modell

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.FødselsnummerGenerator
import java.time.LocalDate

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}

data class TestPerson(
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf(genererIdent(fødselsdato.toLocalDate())),
    var uføre: Int? = null,
    var sykepenger: List<Sykepenger>? = null,
    var foreldrepenger: List<ForeldrePenger>? = null,
) {
    fun aktivIdent(): Ident = identer.find { it.aktivIdent }!!

    data class Sykepenger(val grad: Int, val periode: Periode)
    data class ForeldrePenger(val grad: Number, val periode: Periode)


    fun medSykepenger(sykepenger: List<Sykepenger>): TestPerson {
        this.sykepenger = sykepenger
        return this
    }
}



