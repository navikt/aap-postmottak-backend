package no.nav.aap.behandlingsflyt.flyt.steg.impl

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.student.StudentRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.sykdom.SykdomRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegType
import no.nav.aap.behandlingsflyt.flyt.vilkår.VilkårsresultatRepository
import no.nav.aap.behandlingsflyt.sak.SakService

object VurderSykdomFlytSteg : FlytSteg {

    override fun konstruer(connection: DBConnection): BehandlingSteg {
        return VurderSykdomSteg(
            SykdomRepository(connection),
            StudentRepository(connection),
            VilkårsresultatRepository(connection),
            PeriodeTilVurderingService(SakService(connection))
        )
    }

    override fun type(): StegType {
        return StegType.AVKLAR_SYKDOM
    }
}
