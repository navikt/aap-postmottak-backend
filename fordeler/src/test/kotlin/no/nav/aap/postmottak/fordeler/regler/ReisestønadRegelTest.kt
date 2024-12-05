package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.verdityper.Brevkoder
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ReisestønadRegelTest {


    @ParameterizedTest(name = "Når brevkode er {0} skal regelen evalueres til {1}")
    @CsvSource(value = [
        "SØKNAD_OM_REISESTØNAD,false",
        "SØKNAD_OM_REISESTØNAD_ETTERSENDELSE,false",
        "SØKNAD,true",
        "LEGEERKLÆRING,true",
        "ANNEN,true"
    ])
    fun vurder(brevkode: Brevkoder, forventetResultat: Boolean) {
        val regel = ReisestønadRegel()

        val actual = regel.vurder(ReisestønadRegelInput(brevkode))

        AssertionsForClassTypes.assertThat(actual)
    }


 }