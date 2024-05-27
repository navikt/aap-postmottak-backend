package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.barnetillegg.BarnetilleggService
import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.faktagrunnlag.delvurdering.barnetillegg.BarnetilleggRepository
import no.nav.aap.behandlingsflyt.faktagrunnlag.saksbehandler.barn.BarnVurderingRepository
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakOgBehandlingService
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

class BarnetilleggSteg(private val barnetilleggService: BarnetilleggService) : BehandlingSteg {
    private val log = LoggerFactory.getLogger(BarnetilleggSteg::class.java)

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {

        val barnetillegg = barnetilleggService.beregn(kontekst.behandlingId)

        log.info("Barnetillegg {}", barnetillegg)

        return StegResultat()
    }

    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return BarnetilleggSteg(
                BarnetilleggService(
                    BarnVurderingRepository(connection),
                    BarnetilleggRepository(connection),
                    SakOgBehandlingService(connection)
                )
            )
        }

        override fun type(): StegType {
            return StegType.BARNETILLEGG
        }
    }
}