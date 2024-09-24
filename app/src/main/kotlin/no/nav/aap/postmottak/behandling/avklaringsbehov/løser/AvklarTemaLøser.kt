package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class AvklarTemaLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarTemaLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarTemaLøsning): LøsningsResultat {

        BehandlingRepositoryImpl(connection).lagreTeamAvklaring(kontekst.kontekst.behandlingId, løsning.skalTilAap)

        return LøsningsResultat("Dokumnent er ${if (løsning.skalTilAap) "" else "ikke"} ment for AAP")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_TEMA
    }
}
