package no.nav.aap.postmottak.flyt.flate.visning

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.steg.StegGruppe
import kotlin.reflect.full.primaryConstructor

class DynamiskStegGruppeVisningService(private val connection: DBConnection) {

    private val utledere = mutableMapOf<StegGruppe, StegGruppeVisningUtleder>()

    init {
        StegGruppeVisningUtleder::class.sealedSubclasses.forEach { utleder ->
            val visningUtleder = utleder.primaryConstructor!!.call(connection)
            utledere[visningUtleder.gruppe()] = visningUtleder
        }
    }

    fun skalVises(gruppe: StegGruppe, behandlingId: BehandlingId): Boolean {
        if (!gruppe.skalVises) {
            return false
        }
        if (gruppe.obligatoriskVisning) {
            return true
        }

        val utleder = utledere.getValue(gruppe)

        return utleder.skalVises(behandlingId)
    }
}