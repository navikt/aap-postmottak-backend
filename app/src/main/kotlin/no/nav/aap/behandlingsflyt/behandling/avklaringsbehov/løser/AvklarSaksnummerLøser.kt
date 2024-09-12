package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl

class AvklarSaksnummerLøser(val connection: DBConnection): AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {
    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSaksnummerLøsning): LøsningsResultat {
        BehandlingRepositoryImpl(connection).lagreSaksnummer(kontekst.kontekst.behandlingId, løsning.saksnummer)
        return LøsningsResultat("Saksnummer er valgt")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAKSNUMMER
    }
}