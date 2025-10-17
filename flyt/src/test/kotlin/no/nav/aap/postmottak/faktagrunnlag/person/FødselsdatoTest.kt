package no.nav.aap.postmottak.faktagrunnlag.person

import no.nav.aap.postmottak.faktagrunnlag.register.personopplysninger.Fødselsdato
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class FødselsdatoTest {
    @Test
    fun `tester at man ikke kan sette fødselsdato inn i fremtiden`() {
        assertThrows<IllegalArgumentException> { Fødselsdato(LocalDate.now().plusDays(1)) }
    }
}