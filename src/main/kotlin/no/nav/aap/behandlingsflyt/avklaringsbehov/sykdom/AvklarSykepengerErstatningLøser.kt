package no.nav.aap.behandlingsflyt.avklaringsbehov.sykdom

import no.nav.aap.behandlingsflyt.avklaringsbehov.AvklaringsbehovsLøser
import no.nav.aap.behandlingsflyt.avklaringsbehov.LøsningsResultat
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykepengerErstatningRepository
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst

class AvklarSykepengerErstatningLøser(val connection: DBConnection) : AvklaringsbehovsLøser<AvklarSykepengerErstatningLøsning> {

    private val behandlingRepository = BehandlingRepository(connection)

    override fun løs(kontekst: FlytKontekst, løsning: AvklarSykepengerErstatningLøsning): LøsningsResultat {
        val behandling = behandlingRepository.hent(kontekst.behandlingId)

        SykepengerErstatningRepository.lagre(
            behandlingId = behandling.id,
            vurdering = løsning.vurdering
        )

        return LøsningsResultat(
            begrunnelse = løsning.vurdering.begrunnelse
        )
    }

    override fun forBehov(): Definisjon {
        return Definisjon.AVKLAR_SYKEPENGEERSTATNING
    }
}
