package no.nav.aap.postmottak.gateway

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class AvsenderMottakerDtoTest {

    @Test
    fun `sjekk gyldighet avsendermottaker`() {
        Assertions.assertThat(AvsenderMottakerDto(null, null, null).erGyldig()).isFalse()
        Assertions.assertThat(
            AvsenderMottakerDto(
                null,
                AvsenderMottakerDto.IdType.FNR,
                null
            ).erGyldig()
        ).isFalse()
        Assertions.assertThat(AvsenderMottakerDto("id", null, null).erGyldig()).isFalse()

        Assertions.assertThat(AvsenderMottakerDto(null, null, "Navn Navnesen").erGyldig()).isTrue()
        Assertions.assertThat(
            AvsenderMottakerDto(
                "id",
                AvsenderMottakerDto.IdType.FNR,
                null
            ).erGyldig()
        ).isTrue()

    }
}