package no.nav.aap.postmottak.klient.saf.graphql

import no.nav.aap.postmottak.klient.graphql.asQuery
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(
        val journalpostId: String? = null,
        val fnr: String? = null,
        val dokumentInfoId: String? = null
    )

    companion object {
        fun hentJournalpost(journalpostId: JournalpostId) = SafRequest(
            query = journalpost.asQuery(),
            variables = Variables(journalpostId = journalpostId.toString())
        )

        fun hentSaker(fnr: String) = SafRequest(
            query = saker.asQuery(),
            variables = Variables(fnr = fnr)
        )

        fun hentTilknyttedeJournalposter(dokumentInfoId: DokumentInfoId) = SafRequest(
            query = tilknyttedeJournalposter.asQuery(),
            variables = Variables(dokumentInfoId = dokumentInfoId.dokumentInfoId)
        )
    }
}

private val tilknyttedeJournalposter = $$"""
    query($dokumentInfoId: String!) {
        tilknyttedeJournalposter(dokumentInfoId: $dokumentInfoId, tilknytning: GJENBRUK) {
            journalpostId
            sak {
                fagsakId
                fagsaksystem
                sakstype
            }
            dokumenter {
                dokumentInfoId
            }
        }
    }
""".trimIndent()

private val journalpost = $$"""
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

private val saker = $$"""
    query($fnr: String!) {
        saker(brukerId: {id: $fnr, type: FNR}) {
            tema
            fagsaksystem
        }
    }
""".trimIndent()
