package no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter

import org.assertj.core.api.Assertions
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

internal class KanalFraKodeverkTest {
    @ParameterizedTest
    @EnumSource(KanalFraKodeverk::class, names = ["NAV_NO", "NAV_NO_CHAT", "ALTINN", "EESSI"])
    fun `Sjekk digitale kanaler`(kanal: KanalFraKodeverk) {
        Assertions.assertThat(kanal.erDigitalKanal()).isTrue()
    }

    @ParameterizedTest
    @EnumSource(
        KanalFraKodeverk::class,
        names = ["NAV_NO", "NAV_NO_CHAT", "ALTINN", "EESSI"],
        mode = EnumSource.Mode.EXCLUDE
    )
    fun `Sjekk ikke-digitale kanaler`(kanal: KanalFraKodeverk) {
        Assertions.assertThat(kanal.erDigitalKanal()).isFalse()
    }

}
