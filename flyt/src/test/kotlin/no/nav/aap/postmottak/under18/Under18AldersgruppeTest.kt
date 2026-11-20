package no.nav.aap.postmottak.under18

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class Under18AldersgruppeTest {
    private val fødselsdato = LocalDate.of(2008, 5, 31)

    @Test
    fun `grensene er 17 år og 9 måneder og 18 år`() {
        assertThat(vurder(LocalDate.of(2026, 2, 27))).isEqualTo(Under18Aldersgruppe.FOR_UNG)
        assertThat(vurder(LocalDate.of(2026, 2, 28))).isEqualTo(Under18Aldersgruppe.SAMTYKKE)
        assertThat(vurder(LocalDate.of(2026, 5, 30))).isEqualTo(Under18Aldersgruppe.SAMTYKKE)
        assertThat(vurder(LocalDate.of(2026, 5, 31))).isEqualTo(Under18Aldersgruppe.FYLT_18)
    }

    private fun vurder(dato: LocalDate) = Under18Aldersgruppe.vurder(fødselsdato, dato)
}
