package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class YrkesskadeServiceTest {

    @Test
    fun `Henter yrkesskade`() {
        val yrkesskadeService = YrkesskadeService()

        assertThat(yrkesskadeService.hentYrkesskade()).isEqualTo(Yrkesskadedata())
    }
}
