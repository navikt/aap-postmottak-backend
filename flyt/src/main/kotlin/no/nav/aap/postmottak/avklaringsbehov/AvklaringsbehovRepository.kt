package no.nav.aap.postmottak.avklaringsbehov

import no.nav.aap.lookup.repository.Repository
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.steg.StegType
import java.time.LocalDate

interface AvklaringsbehovRepository: Repository {
    fun hentAvklaringsbehovene(behandlingId: BehandlingId): Avklaringsbehovene
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