package no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer

import no.nav.aap.behandlingsflyt.flyt.Informasjonskrav
import no.nav.aap.behandlingsflyt.flyt.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.Saksnummer
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst
import no.nav.aap.verdityper.sakogbehandling.Ident

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway
): Informasjonskrav {
    companion object: Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): Informasjonskrav {
            return SaksnummerInfoKrav(
                SaksnummerRepository(),
                BehandlingsflytClient()
            )
        }

    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        val saksnummre = behandlingsflytGateway.finnSaker(Ident("yolo", true))

        saksnummerRepository.lagreSaksnummer(kontekst.behandlingId, saksnummre.map { Saksnummer(it.saksnummer)})
        return false
    }

}