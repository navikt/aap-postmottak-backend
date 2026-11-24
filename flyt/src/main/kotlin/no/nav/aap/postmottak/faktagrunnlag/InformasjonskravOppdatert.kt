package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import java.time.Instant

data class InformasjonskravOppdatert(
    val behandlingId: BehandlingId,
    val navn: InformasjonskravNavn,
    val oppdatert: Instant,
    val forrigeInput: String? = null,
) {
    inline fun <reified Input : InformasjonskravInput> forrigeInput(): Input? =
        forrigeInput?.let { DefaultJsonMapper.fromJson<Input>(it) }
}
