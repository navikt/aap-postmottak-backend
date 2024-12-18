package no.nav.aap.postmottak.saf.graphql

import no.nav.aap.postmottak.klient.graphql.asQuery
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(val journalpostId: String? = null, val fnr: String? = null)

    companion object {
        fun hentJournalpost(journalpostId: JournalpostId) = SafRequest(
            query = journalpost.asQuery(),
            variables = Variables(journalpostId = journalpostId.toString())
        )

        fun hentSaker(fnr: String) = SafRequest(
            query = saker.asQuery(),
            variables = Variables(fnr = fnr)
        )
    }
}

private const val journalpostId = "\$journalpostId"

private val journalpost = """
    query($journalpostId: String!) {
        journalpost(journalpostId: $journalpostId) {
            journalpostId
            tittel
            journalposttype
            journalstatus
            tema
            temanavn
            behandlingstema
            behandlingstemanavn
            avsenderMottaker {
                id
                type
            }
            kanal
            relevanteDatoer {
                dato
                datotype
            }
            sak {
                fagsakId
                fagsaksystem
                sakstype
                tema
            }
            bruker {
                id
                type
            }
            journalfoerendeEnhet
            eksternReferanseId
            dokumenter {
                dokumentInfoId
                tittel
                brevkode
                dokumentvarianter {
                    variantformat
                    filtype
                }
            }
        }
    }
""".trimIndent()

private const val fnr = "\$fnr"

private val saker = """
    query($fnr: String!) {
        saker(brukerId: {id: $fnr, type: FNR}) {
            tema
            fagsaksystem
        }
    }
""".trimIndent()
