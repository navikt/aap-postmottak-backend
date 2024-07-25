package no.nav.aap.behandlingsflyt.test

import com.fasterxml.jackson.annotation.JsonProperty


class PersonNavn(
    @JsonProperty("fornavn") val fornavn: String,
    @JsonProperty("etternavn") val etternavn: String
) {
    fun fulltnavn(): String {
        return "$fornavn $etternavn"
    }

    override fun toString(): String {
        return "PersonNavn(fornavn='$fornavn', etternavn='$etternavn')"
    }
}
