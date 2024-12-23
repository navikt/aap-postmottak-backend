package no.nav.aap.postmottak.saf.graphql

import no.nav.aap.postmottak.gateway.SafJournalpost
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.klient.graphql.GraphQLError
import no.nav.aap.postmottak.klient.graphql.GraphQLExtensions

data class SafRespons(
    val data: SafData?,
    val errors: List<GraphQLError>? = null,
    val extensions: GraphQLExtensions? = null,
) {
    fun hasErrors(): Boolean {
        return errors?.isNotEmpty() ?: false
    }
}

data class SafData(
    val journalpost: SafJournalpost?,
    val saker: List<SafSak>?
)
