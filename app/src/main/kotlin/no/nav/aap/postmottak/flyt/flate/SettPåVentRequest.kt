package no.nav.aap.postmottak.flyt.flate

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.tilgang.Journalpostreferanse
import java.time.LocalDate

class SettPåVentRequest(
    val referanse: Long,
    @JsonProperty(
        value = "behandlingVersjon",
        required = true,
        defaultValue = "0"
    ) val behandlingVersjon: Long, val begrunnelse: String, val grunn: ÅrsakTilSettPåVent, val frist: LocalDate?
) : Journalpostreferanse {
    override fun hentAvklaringsbehovKode(): String? {
        return Definisjon.MANUELT_SATT_PÅ_VENT.kode
    }

    override fun hentJournalpostreferanse(): Long {
        return referanse
    }

}