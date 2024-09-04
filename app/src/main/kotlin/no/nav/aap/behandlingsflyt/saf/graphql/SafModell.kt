package no.nav.aap.behandlingsflyt.saf.graphql

import no.nav.aap.behandlingsflyt.graphql.GraphQLError
import no.nav.aap.behandlingsflyt.graphql.GraphQLExtensions

abstract class SafResponse(
    val errors: List<GraphQLError>?,
    val extensions: GraphQLExtensions?
)

class SafDokumentoversiktFagsakDataResponse(
    val data: SafDokumentversiktFagsakData?,
    errors: List<GraphQLError>?,
    extensions: GraphQLExtensions?
) : SafResponse(errors, extensions)

data class Dokumentvariant(val variantformat: Variantformat)
data class Dokument(
    val dokumentInfoId: String,
    val tittel: String,
    val brevkode: String? /* TODO: enum */,
    val dokumentvariant: Dokumentvariant
)

data class Journalpost(val journalpostId: String, val dokumenter: List<Dokument>)
data class DokumentoversiktFagsak(val journalposter: List<Journalpost>)
data class SafDokumentversiktFagsakData(val dokumentoversiktFagsak: DokumentoversiktFagsak?)
enum class Variantformat {
    ARKIV, SLADDET, ORIGINAL
}