package no.nav.aap.domene.behandling

import no.nav.aap.behandlingsflyt.domene.Periode
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkår
import no.nav.aap.behandlingsflyt.domene.behandling.Vilkårstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VilkårTest {

    private val vilkår = Vilkår(Vilkårstype.SYKDOMSVILKÅRET)

    @Test
    fun testJustering() {
        val orginalPeriode = no.nav.aap.behandlingsflyt.domene.Periode(LocalDate.now().minusDays(7), LocalDate.now())
        val nyPeriode =
            no.nav.aap.behandlingsflyt.domene.Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(7))

        val justertPeriode = vilkår.justerPeriode(orginalPeriode, nyPeriode)

        assertThat(justertPeriode).isNotEqualTo(orginalPeriode)
            .isNotEqualTo(nyPeriode)
        assertThat(justertPeriode.overlapper(nyPeriode)).isFalse()
    }
}