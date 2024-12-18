package no.nav.aap.postmottak.fordeler.regler

import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.verdityper.Brevkoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.util.*

class ReisestønadRegelTest {

    @ParameterizedTest(name = "Når brevkode er {0}-{1} skal regelen evalueres til {2}")
    @CsvSource(value = [
        "NAV 11-12.05;SØKNAD_OM_REISESTØNAD;false",
        "NAVe 11-12.05;SØKNAD_OM_REISESTØNAD_ETTERSENDELSE;false",
        "NAV 11-13.05;SØKNAD;true",
        "NAV 08-07.08;LEGEERKLÆRING;true",
        "0000 000;ANNEN;true"
    ], delimiter = ';')
    fun vurder(brevkodeString: String, brevkode: Brevkoder, forventetResultat: Boolean) {
        val regel = ReisestønadRegel()


        val regelInputGenerator = ReisestønadRegelInputGenerator()
        val person = Person(1, UUID.randomUUID(), emptyList())
        val regelInput = regelInputGenerator.generer(RegelInput(1, person, brevkodeString))

        assertThat(regelInput.brevkode).isEqualTo(brevkode)

        val actual = regel.vurder(regelInput)

        assertThat(actual).isEqualTo(forventetResultat)
    }

 }
