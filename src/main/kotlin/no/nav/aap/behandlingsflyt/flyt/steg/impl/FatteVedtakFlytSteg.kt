package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.behandling.BehandlingService
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

object FatteVedtakFlytSteg : FlytSteg {

    override fun konstruer(connection: DbConnection): BehandlingSteg {
        return FatteVedtakSteg(BehandlingService(connection))
    }

    override fun type(): StegType {
        return StegType.FATTE_VEDTAK
    }
}
