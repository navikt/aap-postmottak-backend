package no.nav.aap.postmottak.test.modell

import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.FiktivtNavnGenerator
import no.nav.aap.postmottak.test.FødselsnummerGenerator
import no.nav.aap.postmottak.test.PersonNavn
import java.time.LocalDate

fun genererIdent(fødselsdato: LocalDate): Ident {
    return Ident(FødselsnummerGenerator.Builder().fodselsdato(fødselsdato).buildAndGenerate())
}

class TestPerson(
    val fødselsdato: Fødselsdato = Fødselsdato(LocalDate.now().minusYears(19)),
    val identer: Set<Ident> = setOf(genererIdent(fødselsdato.toLocalDate())),
    val navn: PersonNavn = FiktivtNavnGenerator.genererNavn(),
    var uføre: Int? = null,
) {
    override fun toString(): String {
        return "TestPerson(fødselsdato=$fødselsdato, identer=$identer, navn=$navn"
    }
}
