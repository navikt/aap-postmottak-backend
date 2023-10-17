
package no.nav.aap.behandlingsflyt.flyt.vilkår.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering
import no.nav.aap.behandlingsflyt.flyt.vilkår.Faktagrunnlag
import java.time.LocalDate

class StudentFaktagrunnlag(
    val vurderingsdato: LocalDate,
    val sisteDagMedMuligYtelse: LocalDate,
    val studentvurdering: StudentVurdering
) :
    Faktagrunnlag