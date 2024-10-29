package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.klient.graphql.asQuery

internal data class PdlRequest(val query: String, val variables: Variables) {
    data class Variables(val ident: String? = null, val identer: List<String>? = null)

    companion object {
        fun hentPersonBolk(personidenter: List<String>) = PdlRequest(
            query = PERSON_BOLK_QUERY.asQuery(),
            variables = Variables(identer = personidenter),
        )
        
        fun hentPerson(personident: String) = PdlRequest(
            query = PERSON_QUERY.asQuery(),
            variables = Variables(ident = personident),
        )
        
        fun hentAlleIdenterForPerson(ident: String) = PdlRequest(
            query = IDENT_QUERY.asQuery(),
            variables = Variables(ident = ident),
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

private const val ident = "\$ident"
val PERSON_QUERY = """
    query($ident: ID!) {
        hentPerson(ident: $ident) {
            foedselsdato {
                foedselsdato
            }
        }
    }
""".trimIndent()

val IDENT_QUERY = """
    query($ident: ID!) {
        hentIdenter(ident: $ident, historikk: true) {
            identer {
                ident,
                historisk,
                gruppe
            }
        }
    }
""".trimIndent()
