package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.KategoriserDokumentLøsning
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon

class KategoriserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<KategoriserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: KategoriserDokumentLøsning): LøsningsResultat {

        BehandlingRepositoryImpl(connection).lagreKategoriseringVurdering(kontekst.kontekst.behandlingId, løsning.kategori)

        return LøsningsResultat(løsning.kategori.toString())
    }

    override fun forBehov(): Definisjon {
        return Definisjon.KATEGORISER_DOKUMENT
    }
}
