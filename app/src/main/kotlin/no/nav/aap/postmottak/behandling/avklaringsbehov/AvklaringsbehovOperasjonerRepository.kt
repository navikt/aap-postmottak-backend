package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import java.time.LocalDate

/**
 * Kun for bruk innad i Avklaringsbehovene
 */
interface AvklaringsbehovOperasjonerRepository {
    fun hent(behandlingId: BehandlingId): List<Avklaringsbehov>
    fun opprett(
        behandlingId: BehandlingId,
        definisjon: Definisjon,
        funnetISteg: StegType,
        frist: LocalDate? = null,
        begrunnelse: String = "",
        grunn: ÅrsakTilSettPåVent? = null,
        endretAv: String = SYSTEMBRUKER.ident
    )

    fun kreverToTrinn(avklaringsbehovId: Long, kreverToTrinn: Boolean)
    fun endre(avklaringsbehovId: Long, endring: Endring)
    fun endreVentepunkt(avklaringsbehovId: Long, endring: Endring, funnetISteg: StegType)
}