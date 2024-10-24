package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.klient.graphql.GraphQLError
import no.nav.aap.postmottak.klient.graphql.GraphQLExtensions

data class PersonResultat(
    val ident: String, val navn: List<Navn>, val code: String
)

internal data class PdlResponse(
    val data: PdlData?, val errors: List<GraphQLError>?, val extensions: GraphQLExtensions?
)

data class PdlData(
    val hentPerson: PdlPerson?, val hentPersonBolk: List<HentPersonBolkResult>?
)

data class HentPersonBolkResult(
    val ident: String,
    val person: PdlPerson?,
    val code: String,
)

data class PdlPerson(
    val navn: List<Navn>, val code: Code?     //Denne er påkrevd ved hentPersonBolk
)

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
