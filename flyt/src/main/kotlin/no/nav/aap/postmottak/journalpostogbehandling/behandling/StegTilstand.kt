package no.nav.aap.postmottak.journalpostogbehandling.behandling

import no.nav.aap.postmottak.journalpostogbehandling.flyt.StegStatus
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDateTime

class StegTilstand(
    private val tidspunkt: LocalDateTime = LocalDateTime.now(),
    private val stegStatus: StegStatus,
    private val stegType: StegType,
    var aktiv: Boolean = true
) : Comparable<StegTilstand> {

    fun tidspunkt(): LocalDateTime {
        return tidspunkt
    }
    
    fun status(): StegStatus {
        return stegStatus
    }

    fun steg(): StegType {
        return stegType
    }

    fun deaktiver() {
        this.aktiv = false
    }

    override fun compareTo(other: StegTilstand): Int {
        return tidspunkt.compareTo(other.tidspunkt)
    }

    override fun toString(): String {
        return "StegTilstand(tidspunkt=$tidspunkt, stegStatus=$stegStatus, stegType=$stegType, aktiv=$aktiv)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StegTilstand

        if (stegStatus != other.stegStatus) return false
        if (stegType != other.stegType) return false
        if (aktiv != other.aktiv) return false

        return true
    }

    override fun hashCode(): Int {
        var result = stegStatus.hashCode()
        result = 31 * result + stegType.hashCode()
        result = 31 * result + aktiv.hashCode()
        return result
    }
}
