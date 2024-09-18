package no.nav.aap.behandlingsflyt.forretningsflyt.informasjonskrav.saksnummer

import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskrav
import no.nav.aap.behandlingsflyt.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytClient
import no.nav.aap.behandlingsflyt.overlevering.behandlingsflyt.BehandlingsflytGateway
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.verdityper.flyt.FlytKontekst

class SaksnummerInfoKrav(
    private val saksnummerRepository: SaksnummerRepository,
    private val behandlingsflytGateway: BehandlingsflytGateway
): Informasjonskrav {
    companion object: Informasjonskravkonstruktør {
        override fun konstruer(connection: DBConnection): Informasjonskrav {
            return SaksnummerInfoKrav(
                SaksnummerRepository(connection),
                BehandlingsflytClient()
            )
        }
    }

    override fun harIkkeGjortOppdateringNå(kontekst: FlytKontekst): Boolean {
        // TODO Venter på endrigner i journalpost
        //val saksnummre = behandlingsflytGateway.finnSaker(Ident("PLACEHOLDER", true))

        //saksnummerRepository.lagreSaksnummer(kontekst.behandlingId, saksnummre)
        return true
    }

}