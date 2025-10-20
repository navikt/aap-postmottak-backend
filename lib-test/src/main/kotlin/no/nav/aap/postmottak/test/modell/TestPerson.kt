package no.nav.aap.postmottak.test.modell

import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.FødselsnummerGenerator
import java.time.LocalDate

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}

class TestPerson(
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf(genererIdent(fødselsdato.toLocalDate())),
    var uføre: Int? = null,
) {
    override fun toString(): String {
        return "TestPerson(fødselsdato=$fødselsdato, identer=$identer"
    }

    fun aktivIdent(): Ident = identer.find { it.aktivIdent }!!
}
