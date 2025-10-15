package no.nav.aap.postmottak.gateway

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class AvsenderMottakerDtoTest {

    @Test
    fun `sjekk gyldighet avsendermottaker`() {
        assertThat(AvsenderMottakerDto(null, null, null).erGyldig()).isFalse()
        assertThat(AvsenderMottakerDto(null, AvsenderMottakerDto.IdType.FNR, null).erGyldig()).isFalse()
        assertThat(AvsenderMottakerDto("id", null, null).erGyldig()).isFalse()

        assertThat(AvsenderMottakerDto(null, null, "Navn Navnesen").erGyldig()).isTrue()
        assertThat(AvsenderMottakerDto("id", AvsenderMottakerDto.IdType.FNR, null).erGyldig()).isTrue()

    }
}