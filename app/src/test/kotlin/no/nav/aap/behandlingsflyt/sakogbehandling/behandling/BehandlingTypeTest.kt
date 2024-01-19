package no.nav.aap.behandlingsflyt.sakogbehandling.behandling

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BehandlingTypeTest {
    @Test
    fun name() {
        val utledType = TypeBehandling.from(TypeBehandling.Revurdering.identifikator())

        assertThat(utledType).isEqualTo(TypeBehandling.Revurdering)
    }
}