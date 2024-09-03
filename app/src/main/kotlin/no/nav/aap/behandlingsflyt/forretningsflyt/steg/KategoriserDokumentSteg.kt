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

class KategoriserDokumentSteg private constructor(private val behandlingRepository: BehandlingRepository): BehandlingSteg {
    companion object: FlytSteg {
        override fun konstruer(connection: DBConnection): BehandlingSteg {
            return KategoriserDokumentSteg(BehandlingRepositoryImpl(connection))
        }

        override fun type(): StegType {
            return StegType.KATEGORISER_DOKUMENT
        }

    }

    override fun utfør(kontekst: FlytKontekstMedPerioder): StegResultat {
        /* TODO præv å automatisk kategorisere
        * Dersom automatisk kategorisering er mulig og digitalisering ikke er nødvendig -> send til B-Flow
        * Dersom automatisk kategorisering er mulig og dokumentet må digitaliseres -> send til digitalisering
        * Dersom automatisk kategorisering ikke er mulig  -> avvent kategoriseringsvurdering
        * Dersom manuell vurdering er gjort og digitalisering er nødvendig -> digitaliser
        * Derosm manuell vurdeiring er gjort og digitalisering ikke er nødvendig -> sent til B-Flow
        */
        val behandling = behandlingRepository.hent(kontekst.behandlingId)
        return if (!behandling.harBlittKategorisert()) StegResultat(listOf(Definisjon.KATEGORISER_DOKUMENT))
            else StegResultat()
    }
}