package no.nav.aap.behandlingsflyt.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.behandling.BehandlingId
import no.nav.aap.behandlingsflyt.flyt.steg.StegType

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun hentBehovene(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjon: Definisjon, funnetISteg: StegType)
    fun leggTilAvklaringsbehov(behandlingId: BehandlingId, definisjoner: List<Definisjon>, funnetISteg: StegType)
    fun endreAvklaringsbehov(avklaringsbehovId: Long, status: Status, begrunnelse: String, opprettetAv: String)
    fun endre(avklaringsbehov: Avklaringsbehov)
    fun løs(behandlingId: BehandlingId, definisjon: Definisjon, begrunnelse: String, kreverToTrinn: Boolean?)
    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
}