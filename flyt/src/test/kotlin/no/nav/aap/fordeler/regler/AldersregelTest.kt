package no.nav.aap.fordeler.regler

import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.unleash.FeatureToggle
import no.nav.aap.unleash.PostmottakFeature
import no.nav.aap.unleash.UnleashGateway
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class AldersregelTest {
    private val regel = Aldersregel(Under18Avslått)

    @Test
    fun `Ingen øvre grense for alder til Kelvin`() {
        val fødselsdato = LocalDate.of(1930, 2, 15)
        val nåDato = LocalDate.of(2020, 2, 14)
        assertTrue(regel.vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person yngre enn 18 år skal ikke til Kelvin`() {
        val fødselsdato = LocalDate.of(2002, 2, 16)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertFalse(regel.vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `Journalpost for person som er fylt 18 år skal til Kelvin`() {
        val fødselsdato = LocalDate.of(2002, 2, 15)
        val nåDato = LocalDate.of(2020, 2, 15)
        assertTrue(regel.vurder(AldersregelInput(fødselsdato, nåDato)))
    }

    @Test
    fun `påslått toggle åpner aldersregelen for personer under 18 år`() {
        val input = AldersregelInput(
            LocalDate.of(2009, 2, 15),
            LocalDate.of(2026, 2, 15)
        )

        assertThat(Aldersregel(FakeUnleash).vurder(input)).isTrue()
    }

    @Test
    fun `fylt 18 år passerer aldersregelen også med togglen på`() {
        val input = AldersregelInput(
            LocalDate.of(2008, 2, 15),
            LocalDate.of(2026, 2, 15)
        )

        assertThat(Aldersregel(FakeUnleash).vurder(input)).isTrue()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `manglende fødselsdato gir false uansett toggle`(under18: Boolean) {
        val regel = Aldersregel(if (under18) FakeUnleash else Under18Avslått)
        val input = AldersregelInput(null, LocalDate.of(2026, 2, 15))

        assertThat(regel.vurder(input)).isFalse()
    }
}

private object Under18Avslått : UnleashGateway by FakeUnleash {
    override fun isEnabled(featureToggle: FeatureToggle): Boolean =
        featureToggle != PostmottakFeature.PostmottakUnder18 && FakeUnleash.isEnabled(featureToggle)
}
