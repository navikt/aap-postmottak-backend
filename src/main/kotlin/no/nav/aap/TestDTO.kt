package no.nav.aap

import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import org.jetbrains.annotations.NotNull
import java.time.LocalDate

@Response(statusCode = 202)
data class OpprettTestcaseDTO(
    @JsonProperty(value = "ident", required = true) val ident: String,
    @JsonProperty(value = "fødselsdato", required = true) val fødselsdato: LocalDate,
    @NotNull @JsonProperty(value = "yrkesskade", defaultValue = "false") val yrkesskade: Boolean
)