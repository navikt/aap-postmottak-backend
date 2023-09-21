package no.nav.aap.flate.behandling

import no.nav.aap.domene.Periode
import no.nav.aap.domene.behandling.dokumenter.JournalpostId
import java.time.LocalDate

data class SykdomsGrunnlagDto(
    val opplysninger: InnhentetSykdomsOpplysninger,
    val yrkesskadevurdering: Yrkesskadevurdering?,
    val sykdomsvurdering: Sykdomsvurdering?
)

data class Sykdomsvurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erSkadeSykdomEllerLyteVesentligdel: Boolean,
    val erNedsettelseIArbeidsevneHøyereEnnNedreGrense: Boolean?,
    val nedreGrense: NedreGrense?,
    val skadetidspunkt: LocalDate?
)

enum class NedreGrense {
    TRETTI, FEMTI
}

data class Yrkesskadevurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val erÅrsakssammenheng: Boolean,
    val skadetidspunkt: LocalDate?
)

data class InnhentetSykdomsOpplysninger(
    val oppgittYrkesskadeISøknad: Boolean,
    val innhentedeYrkesskader: List<RegistrertYrkesskade>
)

data class RegistrertYrkesskade(val ref: String, val periode: Periode, val kilde: String)
