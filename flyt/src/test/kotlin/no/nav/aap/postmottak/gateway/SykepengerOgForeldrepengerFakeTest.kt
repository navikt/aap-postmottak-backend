package no.nav.aap.postmottak.gateway

import no.nav.aap.komponenter.type.Periode
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.FakePersoner
import no.nav.aap.postmottak.test.modell.TestPerson
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

@Fakes
class SykepengerOgForeldrepengerFakeTest {

    private val fom = LocalDate.of(2024, 1, 1)
    private val tom = LocalDate.of(2024, 3, 31)

    @Test
    fun `henter sykepengeperioder for en person`(fakePersoner: FakePersoner) {
        val person = TestPerson()
            .medSykepenger(listOf(TestPerson.Sykepenger(grad = 50, periode = Periode(fom, tom))))
        fakePersoner.leggTil(person)
        val ident = person.aktivIdent().identifikator

        val perioder = AbakusSykepengerGateway()
            .hentYtelseSykepenger(setOf(ident), fom, tom)

        assertThat(perioder).hasSize(1)
        assertThat(perioder[0].fom).isEqualTo(fom)
        assertThat(perioder[0].tom).isEqualTo(tom)
        assertThat(perioder[0].grad.toInt()).isEqualTo(50)
    }

    @Test
    fun `person uten sykepenger gir tom liste`(fakePersoner: FakePersoner) {
        val person = TestPerson()
        fakePersoner.leggTil(person)
        val ident = person.aktivIdent().identifikator

        val perioder = AbakusSykepengerGateway()
            .hentYtelseSykepenger(setOf(ident), fom, tom)

        assertThat(perioder).isEmpty()
    }

    @Test
    fun `henter foreldrepengevedtak for en person`(fakePersoner: FakePersoner) {
        val person = TestPerson().apply {
            foreldrepenger = listOf(TestPerson.ForeldrePenger(grad = 80, periode = Periode(fom, tom)))
        }
        fakePersoner.leggTil(person)
        val ident = person.aktivIdent().identifikator

        val response = AbakusForeldrepengerGateway()
            .hentVedtakYtelseForPerson(ForeldrepengerRequest(Aktør(ident), Periode(fom, tom)))

        assertThat(response.ytelser).hasSize(1)
        val ytelse = response.ytelser.single()
        assertThat(ytelse.ytelse).isEqualTo(Ytelser.FORELDREPENGER)
        assertThat(ytelse.anvist).hasSize(1)
        assertThat(ytelse.anvist.single().periode).isEqualTo(Periode(fom, tom))
        assertThat(ytelse.anvist.single().utbetalingsgrad.verdi.toInt()).isEqualTo(80)
    }

    @Test
    fun `person uten foreldrepenger gir tom liste`(fakePersoner: FakePersoner) {
        val person = TestPerson()
        fakePersoner.leggTil(person)
        val ident = person.aktivIdent().identifikator

        val response = AbakusForeldrepengerGateway()
            .hentVedtakYtelseForPerson(ForeldrepengerRequest(Aktør(ident), Periode(fom, tom)))

        assertThat(response.ytelser).isEmpty()
    }
}

