package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe

sealed interface StegGruppeVisningUtleder {

    fun skalVises(behandlingId: BehandlingId): Boolean

    fun gruppe(): StegGruppe
}