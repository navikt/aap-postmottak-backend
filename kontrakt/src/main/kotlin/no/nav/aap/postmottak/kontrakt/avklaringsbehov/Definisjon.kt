package no.nav.aap.postmottak.kontrakt.avklaringsbehov

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.tilgang.Rolle
import java.time.LocalDate
import java.time.Period

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
enum class Definisjon(
    @JsonProperty("kode") val kode: AvklaringsbehovKode,
    val type: BehovType,
    @JsonIgnore private val defaultFrist: Period = Period.ZERO,
    @JsonProperty("løsesISteg") val løsesISteg: StegType = StegType.UDEFINERT,
    val kreverToTrinn: Boolean = false,
    val løsesAv: List<Rolle> = listOf(Rolle.SAKSBEHANDLER_NASJONAL)
) {
    MANUELT_SATT_PÅ_VENT(
        kode = AvklaringsbehovKode.`9001`,
        type = BehovType.VENTEPUNKT,
        defaultFrist = Period.ofWeeks(3),
    ),
    AVKLAR_TEMA(
        kode = AvklaringsbehovKode.`1339`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_TEMA
    ),
    DIGITALISER_DOKUMENT(
        kode = AvklaringsbehovKode.`1338`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.DIGITALISER_DOKUMENT
    ),
    AVKLAR_SAK(
        kode = AvklaringsbehovKode.`1340`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.AVKLAR_SAK
    ),
    AVKLAR_OVERLEVERING(
        kode = AvklaringsbehovKode.`1341`,
        type = BehovType.MANUELT_PÅKREVD,
        løsesISteg = StegType.OVERLEVER_TIL_FAGSYSTEM
    );


    companion object {
        @JsonCreator
        @JvmStatic
        public fun fraKode(@JsonProperty("kode") kode: AvklaringsbehovKode): Definisjon = forKode(kode)
        fun forKode(definisjon: String): Definisjon {
            return entries.single { it.kode == AvklaringsbehovKode.valueOf(definisjon) }
        }

        fun forKode(definisjon: AvklaringsbehovKode): Definisjon {
            return entries.single { it.kode == definisjon }
        }


        init {
            val unikeKoder = entries.map(Definisjon::kode).toSet()

            // Burde dette vært en unit test?
            check(unikeKoder.size == entries.size) { "Gjenbrukt koder for Avklaringsbehov" }

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

    fun erVentebehov(): Boolean {
        return type == BehovType.VENTEPUNKT
    }

    fun utledFrist(frist: LocalDate?): LocalDate {
        check(erVentebehov()) { "Forsøker utlede frist for et behov som ikke er ventepunkt" }
        if (frist != null) {
            return frist
        }
        return LocalDate.now().plus(defaultFrist)
    }

}
