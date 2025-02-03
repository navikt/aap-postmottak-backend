package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.gateway.GeografiskTilknytning
import no.nav.aap.postmottak.gateway.Gradering
import no.nav.aap.postmottak.klient.graphql.GraphQLError
import no.nav.aap.postmottak.klient.graphql.GraphQLExtensions
import java.time.LocalDate

internal data class PdlResponse(
    val data: PdlData?,
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

data class PdlData(
    val hentPerson: HentPersonResult? = null,
    val hentPersonBolk: List<HentPersonBolkResult>? = null,
    val hentIdenter: HentIdenterResult? = null,
    val hentGeografiskTilknytning: GeografiskTilknytning? = null
)

data class HentPersonBolkResult(
    val ident: String,
    val person: PdlPerson?,
    val code: String,
)

data class PdlPerson(
    val navn: List<Navn>, val code: Code?     //Denne er påkrevd ved hentPersonBolk
)

data class HentPersonResult(
    val foedselsdato: List<Fødselsdato>? = null,
    val adressebeskyttelse: List<Gradering>? = null
)

data class Fødselsdato(val foedselsdato: LocalDate, val metadata: HistoriskMetadata)

enum class Code {
    ok, not_found, bad_request
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

data class HistoriskMetadata(val historisk: Boolean)