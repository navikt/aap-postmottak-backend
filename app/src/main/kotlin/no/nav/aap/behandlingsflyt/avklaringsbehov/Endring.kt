package no.nav.aap.behandlingsflyt.avklaringsbehov

import no.nav.aap.behandlingsflyt.avklaringsbehov.løser.ÅrsakTilSettPåVent
import java.time.LocalDate
import java.time.LocalDateTime

class Endring(
    val status: Status,
    val tidsstempel: LocalDateTime = LocalDateTime.now(),
    val begrunnelse: String,
    val grunn: ÅrsakTilSettPåVent? = null,
    val frist: LocalDate? = null,
    val endretAv: String,
    val årsakTilRetur: List<ÅrsakTilRetur> = emptyList()
) : Comparable<Endring> {

    override fun compareTo(other: Endring): Int {
        return tidsstempel.compareTo(other.tidsstempel)
    }
}
