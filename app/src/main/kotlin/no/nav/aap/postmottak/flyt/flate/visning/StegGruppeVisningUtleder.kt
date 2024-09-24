package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import no.nav.aap.verdityper.sakogbehandling.BehandlingId

sealed interface StegGruppeVisningUtleder {

    fun skalVises(behandlingId: BehandlingId): Boolean

    fun gruppe(): StegGruppe
}