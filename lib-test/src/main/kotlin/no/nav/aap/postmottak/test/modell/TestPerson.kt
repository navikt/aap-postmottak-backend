package no.nav.aap.postmottak.test.modell

import no.nav.aap.postmottak.faktagrunnlag.register.barn.Dødsdato
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
    val dødsdato: Dødsdato? = null,
    val barn: List<TestPerson> = emptyList(),
    val navn: PersonNavn = FiktivtNavnGenerator.genererNavn(),
) {
    override fun toString(): String {
        return "TestPerson(fødselsdato=$fødselsdato, identer=$identer, dødsdato=$dødsdato, barn=$barn, navn=$navn"
    }
}
