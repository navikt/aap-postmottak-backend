package no.nav.aap.fordeler.arena

import no.nav.aap.postmottak.Fagsystem
import java.time.LocalDateTime

data class AvklarFordelingVurdering(
    val system: AapSystem,
    val vurdertAv: String,
    val vurdertTidspunkt: LocalDateTime,
    val kommentar: String? = null,
)

enum class AapSystem {
    KELVIN, ARENA, IGNORERT, BEGGE;

    companion object {
        fun fraString(tema: String): AapSystem {
            return when (tema) {
                "KELVIN" -> KELVIN
                "ARENA" -> ARENA
                "BEGGE" -> BEGGE
                "IGNORERT" -> IGNORERT
                else -> IGNORERT
            }
        }
    }

    fun toFagsystem(): Fagsystem? {
        return when (this) {
            KELVIN -> Fagsystem.kelvin
            ARENA -> Fagsystem.arena
            // BEGGE støttes ikke enda – ruting implementeres senere
            else -> null
        }
    }
}