package no.nav.aap.postmottak.test.modell

import no.nav.aap.postmottak.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.sakogbehandling.Ident

class TestOpprettPerson (
    val identer: Set<Ident>,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val barn: List<TestPerson> = emptyList(),
    val yrkesskade:Boolean = false,
)