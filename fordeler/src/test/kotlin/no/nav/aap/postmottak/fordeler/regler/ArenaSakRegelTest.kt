package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.saf.graphql.SafSak
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ArenaSakRegelTest {
    @Test
    fun `Dersom bruker har en sak i Joark med fagsystem AO01 og tema AAP, skal regelen returnere false`() {
        // Arrange
        val input = ArenaSakRegelInput(
            sakerFraArena = emptyList(),
            sakerFraJoark = listOf(
                SafSak(
                    fagsaksystem = "AO01",
                    tema = "AAP"
                ),
                SafSak(
                    fagsaksystem = "AO01",
                    tema = "TSO"
                ),
            )
        )
        val regel = ArenaSakRegel()

        // Act
        val resultat = regel.vurder(input)

        // Assert
        assertFalse(resultat)
    }
}