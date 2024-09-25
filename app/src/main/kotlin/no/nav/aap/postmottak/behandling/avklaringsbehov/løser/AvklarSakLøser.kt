package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.AvklarSaksnummerLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl

class AvklarSakLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarSaksnummerLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarSaksnummerLøsning): LøsningsResultat {

        AvklaringRepositoryImpl(
            connection
        ).lagreSakVurdering(kontekst.kontekst.behandlingId,
            løsning.saksnummer?.let(::Saksnummer)
        )

        return LøsningsResultat("Dokumnent er tildelt sak ${løsning.saksnummer}")
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SAKSNUMMER
    }
}
