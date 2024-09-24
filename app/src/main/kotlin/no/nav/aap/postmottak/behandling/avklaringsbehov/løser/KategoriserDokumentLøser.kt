package no.nav.aap.postmottak.behandling.avklaringsbehov.løser

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.KategoriserDokumentLøsning
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl

class KategoriserDokumentLøser(val connection: DBConnection) : AvklaringsbehovsLøser<KategoriserDokumentLøsning> {

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: KategoriserDokumentLøsning): LøsningsResultat {

        AvklaringRepositoryImpl(connection).lagreKategoriseringVurdering(kontekst.kontekst.behandlingId, løsning.kategori)

        return LøsningsResultat(løsning.kategori.toString())
    }

    override fun forBehov(): Definisjon {
        return Definisjon.KATEGORISER_DOKUMENT
    }
}
