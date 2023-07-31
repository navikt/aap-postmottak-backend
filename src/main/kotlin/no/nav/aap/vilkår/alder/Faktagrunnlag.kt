package no.nav.aap.vilkår.alder

import no.nav.aap.domene.behandling.Faktagrunnlag
import no.nav.aap.domene.behandling.grunnlag.person.Fødselsdato
import java.time.LocalDate

class Aldersgrunnlag(val søknadsdato: LocalDate, val fødselsdato: Fødselsdato) : Faktagrunnlag {
}
