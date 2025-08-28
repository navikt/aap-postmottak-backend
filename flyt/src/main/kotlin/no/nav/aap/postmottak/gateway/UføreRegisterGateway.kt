package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.gateway.Gateway
import java.time.LocalDate

interface UføreRegisterGateway : Gateway {
    fun innhentPerioder(
        fnr: String,
        fraDato: LocalDate
    ): List<Uføre>
}

data class Uføre(
    val virkningstidspunkt: LocalDate,
    val uføregrad: Int?
)

data class UføreRequest (
    val fnr: String,
    val dato: String,
)

data class UføreHistorikkRespons(val uforeperioder: List<UførePeriode>)