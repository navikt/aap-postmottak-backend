package no.nav.aap.postmottak.behandling.avklaringsbehov.flate

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId

@Response(statusCode = 202)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LøsAvklaringsbehovPåBehandling(
    @JsonProperty(value = "referanse", required = true) val referanse: JournalpostId,
    @JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @JsonProperty(value = "behov", required = true) val behov: AvklaringsbehovLøsning,
    @JsonProperty(value = "ingenEndringIGruppe") val ingenEndringIGruppe: Boolean?,
) {
}
