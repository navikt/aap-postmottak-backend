package no.nav.aap.behandlingsflyt.flyt.vilkår

import no.nav.aap.behandlingsflyt.behandling.BehandlingId

object VilkårsresultatRepository {
    private var resultater = HashMap<BehandlingId, Vilkårsresultat>()

    private val LOCK = Object()

    fun lagre(behandlingId: BehandlingId, vilkårsresultat: Vilkårsresultat) {
        synchronized(LOCK) {
            resultater[behandlingId] = vilkårsresultat
        }
    }

    fun hent(behandlingId: BehandlingId): Vilkårsresultat {
        synchronized(LOCK) {
            if (!resultater.containsKey(behandlingId)) {
                resultater[behandlingId] = Vilkårsresultat()
            }
            return resultater.getValue(behandlingId)
        }
    }
}

