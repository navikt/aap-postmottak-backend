package no.nav.aap.postmottak.klient.behandlingsflyt

import com.fasterxml.jackson.annotation.JsonCreator
import no.nav.aap.verdityper.sakogbehandling.Ident
import java.time.LocalDate

data class FinnEllerOpprettSak(
    val ident: String,
    val søknadsdato: LocalDate,
)

data class FinnSaker(val ident: String)

data class SendSøknad(
    val saksnummer: String,
    val journalpostId: String,
    val søknad: Søknad
)

data class Søknad(
    val student: SøknadStudent,
    val yrkesskade: HarYrkesskadeStatus,
    val oppgitteBarn: OppgitteBarn?,
)

data class DigitalSøknadStudent(
    val erStudent: ErStudentStatus? = null,
    val kommeTilbake: SkalGjenopptaStudieStatus? = null
)

data class SøknadStudent(
    val erStudent: ErStudentStatus,
    val kommeTilbake: SkalGjenopptaStudieStatus
)

enum class ErStudentStatus {
    JA,
    NEI,
    AVBRUTT,
    IKKE_OPPGITT;
    
    companion object {
        @JsonCreator
        @JvmStatic
        fun from(value: String): ErStudentStatus {
            return when (value.uppercase()) {
                "JA" -> JA
                "NEI" -> NEI
                "AVBRUTT" -> AVBRUTT
                "IKKE OPPGITT" -> IKKE_OPPGITT
                "IKKE_OPPGITT" -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent enumverdi: $value")
            }
        }
    }
}

enum class SkalGjenopptaStudieStatus {
    JA,
    NEI,
    VET_IKKE,
    IKKE_OPPGITT;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(value: String): SkalGjenopptaStudieStatus {
            return when (value.uppercase()) {
                "JA" -> JA
                "NEI" -> NEI
                "VET IKKE" -> VET_IKKE
                "IKKE OPPGITT" -> IKKE_OPPGITT
                "IKKE_OPPGITT" -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent enumverdi: $value")
            }
        }
    }
}

enum class HarYrkesskadeStatus {
    JA,
    NEI,
    IKKE_OPPGITT;

    companion object {
        @JsonCreator
        @JvmStatic
        fun from(value: String): HarYrkesskadeStatus {
            return when (value.uppercase()) {
                "JA" -> JA
                "NEI" -> NEI
                "IKKE OPPGITT" -> IKKE_OPPGITT
                "IKKE_OPPGITT" -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent enumverdi: $value")
            }
        }
    }
}

// Null-ident representerer at ingen barn er oppgitt i søknad
data class OppgitteBarn(val identer: Set<Ident>?)