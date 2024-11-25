package no.nav.aap.postmottak.klient.pdl

import no.nav.aap.postmottak.klient.graphql.asQuery
import no.nav.aap.verdityper.sakogbehandling.Ident

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

        fun hentAdressebeskyttelseOgGeografiskTilknytning(ident: Ident) = PdlRequest(
            query = ADRESSEBESKYTTELSE_QUERY.asQuery(),
            variables = Variables(ident = ident.identifikator)
        )
        
        fun hentGeografiskTilknytning(ident: String) = PdlRequest(
            query = GEOGRAFISK_TILKNYTNING_QUERY.asQuery(),
            variables = Variables(ident = ident)
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

val ADRESSEBESKYTTELSE_QUERY = """
     query($ident: ID!) {
      hentPerson(ident: $ident) {
        adressebeskyttelse(historikk: false) {
          gradering
        }
      },
    
      hentGeografiskTilknytning(ident: $ident) {
        gtType
        gtKommune
        gtBydel
        gtLand
      }
    }   
""".trimIndent()

val GEOGRAFISK_TILKNYTNING_QUERY = """
    query($ident: ID!) {
        hentGeografiskTilknytning(ident: $ident) {
            gtType
            gtKommune
            gtBydel
            gtLand
        }
}
""".trimIndent()