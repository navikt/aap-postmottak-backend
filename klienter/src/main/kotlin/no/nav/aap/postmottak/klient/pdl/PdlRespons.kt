package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.klient.graphql.GraphQLError
import no.nav.aap.postmottak.klient.graphql.GraphQLExtensions
import no.nav.aap.postmottak.klient.norg.Diskresjonskode
import java.time.LocalDate

internal data class PdlResponse(
    val data: PdlData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class PdlData(
    val hentPerson: HentPersonResult?,
    val hentPersonBolk: List<HentPersonBolkResult>?,
    val hentIdenter: HentIdenterResult?,
    val hentGeografiskTilknytning: GeografiskTilknytning?,
)

data class HentPersonBolkResult(
    val ident: String,
    val person: PdlPerson?,
    val code: String,
)

data class GeografiskTilknytning(
    val gtType: GeografiskTilknytningType,
    val gtKommune: String?,
    val gtBydel: String?,
    val gtLand: String?,
    val regel: String,
)

enum class GeografiskTilknytningType{
    KOMMUNE,
    BYDEL,
    UTLDAND,
    UDEFINERT
}

data class PdlPerson(
    val navn: List<Navn>, val code: Code?     //Denne er påkrevd ved hentPersonBolk
)

data class HentPersonResult(
    val foedselsdato: List<Fødselsdato>?,
    val adressebeskyttelse: List<Adressebeskyttelseskode>?
)

data class Fødselsdato(val foedselsdato: LocalDate)

enum class Code {
    ok, not_found, bad_request //TODO: add more
}

data class Navn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?
) {
    fun fulltNavn(): String {
        return "${fornavn ?: ""} ${mellomnavn ?: ""} ${etternavn ?: ""}".trim()
    }
}

data class HentIdenterResult(val identer: List<PdlIdent>)

data class PdlIdent(
    val ident: String,
    val historisk: Boolean,
    val gruppe: PdlGruppe
)

enum class PdlGruppe {
    FOLKEREGISTERIDENT,
    AKTORID,
    NPID,
}

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