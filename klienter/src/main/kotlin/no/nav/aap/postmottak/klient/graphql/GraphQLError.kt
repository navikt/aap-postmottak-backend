package no.nav.aap.postmottak.klient.graphql

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLError(
    val message: String,
    val path: List<String>?,
    val extensions: GraphQLErrorExtension
)

data class GraphQLErrorExtension(
    val code: ErrorCode?,
    val classification: String
)

enum class ErrorCode {
    FORBIDDEN,
    NOT_FOUND,
    BAD_REQUEST,
    SERVER_ERROR,
    ;

    // SAF sine feilkoder kommer i små bokstaver
    companion object {
        @JvmStatic
        @JsonCreator
        fun fra(value: String?) = entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

data class GraphQLExtensions(
    val warnings: List<GraphQLWarning>?
)

class GraphQLWarning(
    val query: String?,
    val id: String?,
    val code: String?,
    val message: String?,
    val details: String?,
)
