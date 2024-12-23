package no.nav.aap.postmottak.gateway

import no.nav.aap.fordeler.Diskresjonskode
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import java.time.LocalDate

interface PersondataGateway: Gateway {
    fun hentPersonBolk(
        personidenter: List<String>
    ): Map<String, Navn>?

    fun hentFødselsdato(
        personident: String
    ): LocalDate?

    fun hentGeografiskTilknytning(
        personident: String,
    ): GeografiskTilknytning?

    fun hentAlleIdenterForPerson(ident: String): List<Ident>

    fun hentAdressebeskyttelseOgGeolokasjon(ident: Ident): GeografiskTilknytningOgAdressebeskyttelse
}

data class Navn(val verdi: String?)

data class GeografiskTilknytning(
    val gtType: GeografiskTilknytningType,
    val gtKommune: String? = null,
    val gtBydel: String? = null,
    val gtLand: String? = null,
)

enum class GeografiskTilknytningType{
    KOMMUNE,
    BYDEL,
    UTLAND,
    UDEFINERT
}

data class GeografiskTilknytningOgAdressebeskyttelse(
    val geografiskTilknytning: GeografiskTilknytning,
    val adressebeskyttelse: List<Adressebeskyttelseskode>
)

enum class Adressebeskyttelseskode {
    FORTROLIG,
    STRENGT_FORTROLIG,
    STRENGT_FORTROLIG_UTLAND,
    UGRADERT;

    fun tilDiskresjonskode() =
        when (this) {
            FORTROLIG -> Diskresjonskode.SPFO
            STRENGT_FORTROLIG, STRENGT_FORTROLIG_UTLAND -> Diskresjonskode.SPSF
            UGRADERT -> Diskresjonskode.ANY
        }
}