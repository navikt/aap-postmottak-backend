package no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.student

class StudentGrunnlag(
    val id: Long? = null,
    val studentvurdering: no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.student.StudentVurdering?,
) {
    fun erKonsistent(): Boolean {
        // TODO: Kommenter inn når lagring er på plass
//        if (oppgittStudent == null) {
//            return true
//        }
        return studentvurdering != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as no.nav.aap.behandlingsflyt.faktagrunnlag.usorterte.student.StudentGrunnlag

        return studentvurdering == other.studentvurdering
    }

    override fun hashCode(): Int {
        return studentvurdering?.hashCode() ?: 0
    }
}
