package no.nav.aap.behandlingsflyt.test.modell

import no.nav.aap.behandlingsflyt.faktagrunnlag.register.barn.Dødsdato
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.inntekt.InntektPerÅr
import no.nav.aap.behandlingsflyt.faktagrunnlag.register.personopplysninger.Fødselsdato
import no.nav.aap.verdityper.sakogbehandling.Ident

class TestPerson (
    val identer: Set<Ident>,
    val fødselsdato: Fødselsdato,
    val dødsdato: Dødsdato? = null,
    val barn: List<TestPerson> = emptyList(),
    val yrkesskade:List<TestYrkesskade> = emptyList(),
    val inntekter: List<InntektPerÅr> = emptyList()
)