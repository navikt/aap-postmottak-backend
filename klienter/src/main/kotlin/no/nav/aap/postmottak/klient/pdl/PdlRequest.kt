package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.klient.graphql.asQuery

internal data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String? = null, val identer: List<String>? = null)

    companion object {
        fun hentPersonBolk(personidenter: List<String>) = PdlRequest(
            query = PERSON_BOLK_QUERY.asQuery(),
            variables = Variables(identer = personidenter),
        )
    }
}


private const val identer = "\$identer"
val PERSON_BOLK_QUERY = """
    query($identer: [ID!]!) {
        hentPersonBolk(identer: $identer) {
            ident,
                person {
                    navn {
                        fornavn
                        mellomnavn
                        etternavn
                    }
                },
            code
        }
    }
""".trimIndent()