package no.nav.aap.domene.behandling

import no.nav.aap.domene.Periode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårTest {

    private val vilkår = Vilkår(Vilkårstype.SYKDOMSVILKÅRET)

    @Test
    fun testJustering() {
        val orginalPeriode = Periode(LocalDate.now().minusDays(7), LocalDate.now())
        val nyPeriode = Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(7))

        val justertPeriode = vilkår.justerPeriode(orginalPeriode, nyPeriode)

        assertThat(justertPeriode).isNotEqualTo(orginalPeriode)
            .isNotEqualTo(nyPeriode)
        assertThat(justertPeriode.overlapper(nyPeriode)).isFalse()
    }
}