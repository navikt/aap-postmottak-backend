package no.nav.aap.behandlingsflyt.faktagrunnlag.yrkesskade

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class YrkesskadedatalagerTest {

    @Test
    fun `Data er kun oppdaterte dersom de er lagret i dag`() {
        val datalager = Yrkesskadedatalager()

        assertThat(datalager.erOppdatert()).isFalse

        datalager.lagre(Yrkesskadedata(), LocalDate.now().atStartOfDay().minusNanos(1))

        assertThat(datalager.erOppdatert()).isFalse

        datalager.lagre(Yrkesskadedata(), LocalDateTime.now())

        assertThat(datalager.erOppdatert()).isTrue
    }

    @Test
    fun `Henting av data før lagring returnerer null`() {
        val datalager = Yrkesskadedatalager()

        assertThat(datalager.hentYrkesskade()).isNull()

        datalager.lagre(Yrkesskadedata(), LocalDateTime.now())

        assertThat(datalager.hentYrkesskade()).isEqualTo(Yrkesskadedata())
    }
}
