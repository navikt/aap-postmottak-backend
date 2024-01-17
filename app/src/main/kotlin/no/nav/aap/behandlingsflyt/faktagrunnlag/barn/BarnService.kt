package no.nav.aap.behandlingsflyt.faktagrunnlag.barn

import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlag
import no.nav.aap.behandlingsflyt.faktagrunnlag.Grunnlagkonstruktør
import no.nav.aap.behandlingsflyt.faktagrunnlag.barn.adapter.BarnRelasjonerMock
import no.nav.aap.behandlingsflyt.flyt.FlytKontekst
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService

class BarnService private constructor(private val connection: DBConnection) : Grunnlag {

    companion object : Grunnlagkonstruktør {
        override fun konstruer(connection: DBConnection): BarnService {
            return BarnService(connection)
        }
    }

    override fun oppdater(kontekst: FlytKontekst): Boolean {
        val personopplysningRepository = BarnRepository(connection)
        val sakService = SakService(connection)
        val sak = sakService.hent(kontekst.sakId)
        val behandlingId = kontekst.behandlingId

        val harBehandlingsgrunnlag = vurderBehandlingsgrunnlag(behandlingId)

        val barn = if (harBehandlingsgrunnlag) {
            BarnRelasjonerMock.innhent(sak.person.identer(), sak.rettighetsperiode)
        } else {
            listOf()
        }

        val gamleData = personopplysningRepository.hentHvisEksisterer(behandlingId)

        personopplysningRepository.lagre(behandlingId, barn)
        val nyeData = personopplysningRepository.hentHvisEksisterer(behandlingId)

        return nyeData == gamleData
    }

    private fun vurderBehandlingsgrunnlag(behandlingId: BehandlingId): Boolean {
        // TODO: Avgjøre om man har hjemmel til å innhente (dvs er det innvilget)
        return true
    }
}
