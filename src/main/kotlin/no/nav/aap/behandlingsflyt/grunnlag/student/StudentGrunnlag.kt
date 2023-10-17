package no.nav.aap.behandlingsflyt.grunnlag.student

import no.nav.aap.behandlingsflyt.avklaringsbehov.student.StudentVurdering

class StudentGrunnlag(
        val id: Long,
        val behandlingId: Long,
        val studentvurdering: StudentVurdering?,
) {
    fun erKonsistent(): Boolean {
        return studentvurdering != null
    }
}
