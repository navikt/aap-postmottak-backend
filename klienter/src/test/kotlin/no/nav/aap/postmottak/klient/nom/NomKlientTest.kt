package no.nav.aap.postmottak.klient.nom

import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.SKJERMET_IDENT
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

@Fakes
class NomKlientTest {

    @Test
    fun erEgenAnsatt() {
        val client = NomKlient()
        val actual = client.erEgenAnsatt(SKJERMET_IDENT)

        assertThat(actual).isTrue()
    }

    @Test
    fun erIkkeEgenansatt() {
        val client = NomKlient()

        val actual = client.erEgenAnsatt(Ident("123412341243"))

        assertThat(actual).isFalse()
    }

}