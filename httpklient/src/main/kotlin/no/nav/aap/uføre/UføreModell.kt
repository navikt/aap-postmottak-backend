package no.nav.aap.uføre

enum class SakTypeCode {
    UFORE
}

class UføreRequest (
    val fnr: List<String>,
    val fom: String,
    val sakstype:SakTypeCode = SakTypeCode.UFORE
)
