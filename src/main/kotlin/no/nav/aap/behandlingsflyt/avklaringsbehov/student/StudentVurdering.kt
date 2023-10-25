
package no.nav.aap.behandlingsflyt.avklaringsbehov.student

import no.nav.aap.behandlingsflyt.behandling.dokumenter.JournalpostId
import java.time.LocalDate

data class StudentVurdering(
    val begrunnelse: String,
    val dokumenterBruktIVurdering: List<JournalpostId>,
    val oppfyller11_14: Boolean?,
    val avbruttStudieDato: LocalDate?
)
