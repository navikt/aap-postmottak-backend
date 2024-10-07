package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.SattPåVentLøsning
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class SattPåVentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<SattPåVentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: SattPåVentLøsning): LøsningsResultat {
        return LøsningsResultat("Tatt av vent")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.MANUELT_SATT_PÅ_VENT
    }
}