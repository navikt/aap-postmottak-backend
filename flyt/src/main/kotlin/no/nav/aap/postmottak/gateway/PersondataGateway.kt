package no.nav.aap.postmottak.gateway

import no.nav.aap.fordeler.Diskresjonskode
import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import java.time.LocalDate

interface PersondataGateway: Gateway {
    fun hentPersonBolk(
        personidenter: List<String>
    ): Map<String, NavnMedIdent?>?

    fun hentFødselsdato(
        personident: String
    ): LocalDate?
    
    fun hentNavn(
        personident: String
    ): Navn?

    fun hentGeografiskTilknytning(
        personident: String,
    ): GeografiskTilknytning?

    fun hentAlleIdenterForPerson(ident: String): List<Ident>

    fun hentAdressebeskyttelseOgGeolokasjon(ident: Ident): GeografiskTilknytningOgAdressebeskyttelse
}

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
    val adressebeskyttelse: List<Gradering>
)

data class Gradering(
    val gradering: Adressebeskyttelseskode
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

data class Navn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
) {
    fun fulltNavn(): String {
        return listOfNotNull(fornavn, mellomnavn, etternavn).joinToString(" ")
    }
}

data class Identifikasjonsnummer (
    val identifikasjonsnummer: String
)

data class NavnMedIdent(
    val navn: Navn?,
    val ident: String?
)