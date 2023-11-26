package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

interface AvklaringsbehovRepository {
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjoner: List<Definisjon>, funnetISteg: StegType)
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType)
    fun løs(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, kreverToTrinn: Boolean?)
    fun hent(behandlingId: BehandlingId): Avklaringsbehovene
    fun opprettAvklaringsbehovEndring(avklaringsbehovId: Long, status: Status, begrunnelse: String, opprettetAv: String)
}