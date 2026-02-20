package no.nav.aap.postmottak.api.flyt

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.papsign.ktor.openapigen.annotations.Response
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklaringsbehovLøsning
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.plugin.kontrakt.Journalpostreferanse

@Response(statusCode = 202)
@JsonIgnoreProperties(ignoreUnknown = true)
data class LøsAvklaringsbehovPåBehandling(
    @param:JsonProperty(value = "referanse", required = true) val referanse: Behandlingsreferanse,
    @param:JsonProperty(value = "behandlingVersjon", required = true, defaultValue = "0") val behandlingVersjon: Long,
    @param:JsonProperty(value = "behov", required = true) val behov: `AvklaringsbehovLøsning`,
    @param:JsonProperty(value = "ingenEndringIGruppe") val ingenEndringIGruppe: Boolean?,
) : Journalpostreferanse {
    override fun hentAvklaringsbehovKode(): String {
        return behov.definisjon().kode.name
    }

    override fun journalpostIdResolverInput(): String {
        return referanse.referanse.toString()
    }

}