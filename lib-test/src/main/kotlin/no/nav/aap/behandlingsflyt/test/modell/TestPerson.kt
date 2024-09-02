package no.nav.aap.behandlingsflyt.test.modell

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.behandlingsflyt.test.FiktivtNavnGenerator
import no.nav.aap.behandlingsflyt.test.FødselsnummerGenerator
import no.nav.aap.behandlingsflyt.test.PersonNavn
import no.nav.aap.verdityper.Prosent
import no.nav.aap.verdityper.sakogbehandling.Ident
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
    val yrkesskade: List<TestYrkesskade> = emptyList(),
    val uføre: Prosent = Prosent(0),
) {


    override fun toString(): String {
        return "TestPerson(fødselsdato=$fødselsdato, identer=$identer, dødsdato=$dødsdato, barn=$barn, navn=$navn, yrkesskade=$yrkesskade"
    }
}
