package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.verdityper.flyt.StegType
import java.time.LocalDate
import java.time.Period
import java.util.*
import java.util.stream.Collectors


const val KATEGORISER_DOKUMENT_KODE = "1337"
const val DIGITALISER_DOKUMENT_KODE = "1338"

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Definisjon(
    @JsonProperty("kode") val kode: String,
    val type: BehovType,
    @JsonProperty("løsesISteg") val løsesISteg: StegType = StegType.UDEFINERT,
    val kreverToTrinn: Boolean = false
) {
    KATEGORISER_DOKUMENT(
        kode =  KATEGORISER_DOKUMENT_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.KATEGORISER_DOKUMENT
    ),
    DIGITALISER_DOKUMENT(
        kode = DIGITALISER_DOKUMENT_KODE,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DIGITALISER_DOKUMENT
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
        MANUELT_FRIVILLIG(Definisjon::validerManuelt)
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

    override fun toString(): String {
        return "$name(kode='$kode')"
    }

    fun erFrivillig(): Boolean {
        return type == BehovType.MANUELT_FRIVILLIG
    }

}
