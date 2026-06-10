package no.nav.aap.fordeler.arena

import no.nav.aap.postmottak.Fagsystem
import java.time.LocalDateTime

data class AvklarFordelingVurdering(
    val system: AapSystem,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime,
)

enum class AapSystem {
    KELVIN, ARENA, IGNORERT;

    companion object {
        fun fraString(tema: String): AapSystem {
            return when (tema) {
                "KELVIN" -> KELVIN
                "ARENA" -> ARENA
                "IGNORERT" -> IGNORERT
                else -> IGNORERT
            }
        }
    }

    fun toFagsystem(): Fagsystem? {
        return when (this) {
            KELVIN -> Fagsystem.kelvin
            ARENA -> Fagsystem.arena
            else -> null
        }
    }
}