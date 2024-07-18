package no.nav.aap.medlemskap

data class MedlemskapRequest(
    val fodselsnumre: List<String>,
)

data class MedlemskapResponse(
    val unntakId: Number,
    val ident: String,
    val fraOgMed: String,
    val tilOgMed: String,
    val status: String,
    val statusaarsak: String?,
    val medlem: Boolean
)