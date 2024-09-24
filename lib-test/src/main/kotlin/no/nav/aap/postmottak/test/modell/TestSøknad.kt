package no.nav.aap.postmottak.test.modell

import no.nav.aap.postmottak.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.sakogbehandling.Ident

class TestSøknad(
    val identer: Set<Ident>,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val barn: List<TestSøknad> = emptyList(),
    val yrkesskade:List<TestYrkesskade> = emptyList(),
) {
    init {
        require(identer.isNotEmpty())
    }

    fun aktivIdent(): String {
        return identer.single { it.aktivIdent }.identifikator
    }
}