package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarTemaLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer

class AvklarSakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSaksnummerLøsning): LøsningsResultat {

        BehandlingRepositoryImpl(connection).lagreSakVurdeirng(kontekst.kontekst.behandlingId,
            løsning.saksnummer?.let(::Saksnummer)
        )

        return LøsningsResultat("Dokumnent er tildelt sak ${løsning.saksnummer}")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAKSNUMMER
    }
}
