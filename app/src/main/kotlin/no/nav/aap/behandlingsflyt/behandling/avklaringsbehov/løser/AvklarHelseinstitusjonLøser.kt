package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.løsning.AvklarHelseinstitusjonLøsning
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.institusjon.HelseinstitusjonRepository

class AvklarHelseinstitusjonLøser(connection: DBConnection) : AvklaringsbehovsLøser<AvklarHelseinstitusjonLøsning> {

    private val helseinstitusjonRepository = HelseinstitusjonRepository(connection)

    override fun løs(kontekst: AvklaringsbehovKontekst, løsning: AvklarHelseinstitusjonLøsning): LøsningsResultat {
        helseinstitusjonRepository.lagre(kontekst.kontekst.behandlingId, løsning.helseinstitusjonVurdering.tilDomeneobjekt())
        return LøsningsResultat(løsning.helseinstitusjonVurdering.begrunnelse)
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_HELSEINSTITUSJON
    }
}