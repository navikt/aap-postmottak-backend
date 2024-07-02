package no.nav.aap.medlemskap

data class MedlemskapRequest (
    val fodselsnumre: List<String>,
)

class MedlemskapResponse (
    val unntak: List<Unntak>
)

class Unntak (
    val unntakId: Number,
    val ident: String,
)