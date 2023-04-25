package no.nav.aap.flyt

class Tilstand(private val type: StegType, private val status: StegStatus) {

    fun status(): StegStatus {
        return status
    }

    fun steg(): StegType {
        return type
    }

    override fun toString(): String {
        return "Tilstand(type=$type, status=$status)"
    }
}
