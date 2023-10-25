package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbstuff.DbConnection
import no.nav.aap.behandlingsflyt.domene.behandling.BehandlingTjeneste
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.db.InMemoryStudentRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

object VurderSykdomFlytSteg : FlytSteg {

    override fun konstruer(connection: DbConnection): BehandlingSteg {
        return VurderSykdomSteg(BehandlingTjeneste, InMemoryStudentRepository)
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}
