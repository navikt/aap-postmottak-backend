package no.nav.aap.postmottak.klient.saf.graphql

import no.nav.aap.postmottak.klient.graphql.asQuery
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

internal data class SafRequest(val query: String, val variables: Variables) {
    data class Variables(
        val journalpostId: String? = null,
        val fnr: String? = null,
        val dokumentInfoId: String? = null,
        val antall: Int? = null,
        val etter: String? = null
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

        fun hentAapJournalposter(fnr: String, antall: Int, etter: String?) = SafRequest(
            query = dokumentoversiktBruker.asQuery(),
            variables = Variables(fnr = fnr, antall = antall, etter = etter)
        )

        fun hentTilknyttedeJournalposter(dokumentInfoId: DokumentInfoId) = SafRequest(
            query = tilknyttedeJournalposter.asQuery(),
            variables = Variables(dokumentInfoId = dokumentInfoId.dokumentInfoId)
        )
    }
}

private val dokumentoversiktBruker = $$"""
    query($fnr: String!, $antall: Int!, $etter: String) {
        dokumentoversiktBruker(
            brukerId: {id: $fnr, type: FNR},
            tema: [AAP],
            journalstatuser: [],
            foerste: $antall,
            etter: $etter
        ) {
            journalposter {
                journalpostId
                tittel
                datoOpprettet
                tema
                skjerming
                dokumenter {
                    dokumentInfoId
                    tittel
                    skjerming
                    dokumentvarianter {
                        variantformat
                        saksbehandlerHarTilgang
                        skjerming
                    }
                }
            }
            sideInfo {
                sluttpeker
                finnesNesteSide
            }
        }
    }
""".trimIndent()

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
