package no.nav.aap.behandlingsflyt.forretningsflyt.steg

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Definisjon
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.flyt.steg.BehandlingSteg
import no.nav.aap.behandlingsflyt.flyt.steg.FlytSteg
import no.nav.aap.behandlingsflyt.flyt.steg.StegResultat
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.verdityper.flyt.FlytKontekstMedPerioder
import no.nav.aap.verdityper.flyt.StegType
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(GrovkategoriseringSteg::class.java)

class GrovkategoriseringSteg(private val behandlingRepository: BehandlingRepository) : BehandlingSteg {
    companion object : FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return GrovkategoriseringSteg(BehandlingRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.GROVKATEGORTISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        log.info("Treffer Grovkategoriseringssteg")
        /* TODO finn avklaring om dokument faktisk skal til AAP eller skal returneres
        *  Hvis dokument er avklart med ja: Stegresultat()
        *  Hvis avklart med nei: Returner avklaringsbehov for returnering av dokument
        *  Hvis ikke avklart enda: returner Definisjon.GROVKATEGORISER_DOKUMENT
        */
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        return if (!behandling.harBlittgrovkategorisert()) StegResultat(listOf(Definisjon.GROVKATEGORISER_DOKUMENT))
            else if (behandling.vurderinger.grovkategorivurdering!!.vurdering) StegResultat()
            else StegResultat(listOf(/*Definisjon.RETURNERINGSPROSEDYRE*/))
    }
}