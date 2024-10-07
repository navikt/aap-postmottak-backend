package no.nav.aap.postmottak.kontrakt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors

const val MANUELT_SATT_PÅ_VENT_KODE = "9001"
const val KATEGORISER_DOKUMENT_KODE = "1337"
const val DIGITALISER_DOKUMENT_KODE = "1338"
const val AVKLAR_TEMA_KODE = "1339"
const val AVKLAR_SAKSNUMMER_KODE = "1340"

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Definisjon(
    @JsonProperty("kode") val kode: String,
    val type: BehovType,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @JsonProperty("løsesISteg") val løsesISteg: StegType = StegType.UDEFINERT,
    val kreverToTrinn: Boolean = false
) {
    MANUELT_SATT_PÅ_VENT(
        kode = MANUELT_SATT_PÅ_VENT_KODE,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(3),
    ),
    AVKLAR_TEMA(
        kode = AVKLAR_TEMA_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_TEMA
    ),
    KATEGORISER_DOKUMENT(
        kode =  KATEGORISER_DOKUMENT_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KATEGORISER_DOKUMENT
    ),
    DIGITALISER_DOKUMENT(
        kode = DIGITALISER_DOKUMENT_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DIGITALISER_DOKUMENT
    ),
    AVKLAR_SAK(
        kode = AVKLAR_SAKSNUMMER_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_SAK
    );

    companion object {
        fun forKode(definisjon: String): Definisjon {
            return entries.single { it.kode == definisjon }
        }

        init {
            val unikeKoder =
                Arrays.stream(entries.toTypedArray())
                    .map { it.kode }
                    .collect(Collectors.toSet())

            if (unikeKoder.size != entries.size) {
                throw IllegalStateException("Gjenbrukt koder for Avklaringsbehov")
            }

            for (value in entries) {
                value.type.valideringsFunksjon(value)
            }
        }
    }

    enum class BehovType(val valideringsFunksjon: Definisjon.() -> Unit) {
        MANUELT_PÅKREVD(Definisjon::validerManuelt),
        MANUELT_FRIVILLIG(Definisjon::validerManuelt),
        VENTEPUNKT(Definisjon::validerVentepunkt)
    }

    fun skalLøsesISteg(steg: StegType, funnetISteg: StegType): Boolean {
        if (løsesISteg == StegType.UDEFINERT) {
            return steg == funnetISteg
        }
        return løsesISteg == steg
    }

    private fun validerManuelt() {
        if (this.løsesISteg.tekniskSteg) {
            throw IllegalArgumentException(
                "Avklaringsbehov må være knyttet til et funksjonelt steg"
            )
        }
    }

    private fun validerVentepunkt() {
        if (this == MANUELT_SATT_PÅ_VENT) {
            if (this.løsesISteg != StegType.UDEFINERT) {
                throw IllegalArgumentException("Manueltsatt på vent er lagt til feil steg")
            }
        }
        if (this != MANUELT_SATT_PÅ_VENT) {
            if (this.løsesISteg == StegType.UDEFINERT) {
                throw IllegalArgumentException("Ventepunkt er lagt til feil steg")
            }
            if (defaultFrist == Period.ZERO) {
                throw IllegalArgumentException("Vent trenger å sette en default frist")
            }
        }
    }

    override fun toString(): String {
        return "$name(kode='$kode')"
    }

    fun erFrivillig(): Boolean {
        return type == BehovType.MANUELT_FRIVILLIG
    }

    fun erVentepunkt(): Boolean {
        return type == BehovType.VENTEPUNKT
    }

    fun utledFrist(frist: LocalDate?): LocalDate {
        if (!erVentepunkt()) {
            throw IllegalStateException("Forsøker utlede frist for et behov som ikke er ventepunkt")
        }
        if (frist != null) {
            return frist
        }
        return LocalDate.now().plus(defaultFrist)
    }

}
