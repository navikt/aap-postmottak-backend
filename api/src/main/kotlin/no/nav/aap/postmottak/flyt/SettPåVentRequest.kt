package no.nav.aap.postmottak.flyt

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandlingsreferanse
import no.nav.aap.tilgang.plugin.kontrakt.Journalpostreferanse
import java.time.LocalDate

data class SettPåVentRequest(
    val referanse: Behandlingsreferanse,
    @JsonProperty(
        value = "behandlingVersjon",
        required = true,
        defaultValue = "0"
    ) val behandlingVersjon: Long, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate?
): Journalpostreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return Definisjon.MANUELT_SATT_PÅ_VENT.kode.name
    }

    override fun journalpostIdResolverInput(): String {
        return referanse.referanse.toString()
    }

}