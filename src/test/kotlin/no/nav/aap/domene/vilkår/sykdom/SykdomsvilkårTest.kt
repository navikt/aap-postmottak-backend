package no.nav.aap.domene.vilkår.sykdom

import no.nav.aap.avklaringsbehov.sykdom.NedreGrense
import no.nav.aap.avklaringsbehov.sykdom.Sykdomsvurdering
import no.nav.aap.domene.behandling.Utfall
import no.nav.aap.domene.behandling.Vilkår
import no.nav.aap.domene.behandling.Vilkårstype
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate


class SykdomsvilkårTest {

    @Test
    fun `testNye vurderinger skal overskrive`() {
        val vilkår = Vilkår(Vilkårstype.SYKDOMSVILKÅRET)

        Sykdomsvilkår(vilkår).vurder(
            SykdomsFaktagrunnlag(
                LocalDate.now(),
                LocalDate.now().plusYears(3),
                null,
                Sykdomsvurdering("", listOf(), true, true, NedreGrense.FEMTI, LocalDate.now().minusYears(1))
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.OPPFYLT }

        Sykdomsvilkår(vilkår).vurder(
            SykdomsFaktagrunnlag(
                LocalDate.now(),
                LocalDate.now().plusYears(3),
                null,
                Sykdomsvurdering("", listOf(), true, false, NedreGrense.FEMTI, LocalDate.now().minusYears(1))
            )
        )

        assertThat(vilkår.vilkårsperioder()).hasSize(1).allMatch { periode -> periode.utfall == Utfall.IKKE_OPPFYLT }
    }
}