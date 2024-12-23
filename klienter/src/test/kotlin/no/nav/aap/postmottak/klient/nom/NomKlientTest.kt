package no.nav.aap.postmottak.klient.nom

import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.test.fakes.nomFake
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class NomKlientTest: WithFakes {

    @Test
    fun erEgenAnsatt() {
        val client = NomKlient()

        WithFakes.fakes.nomFake.setCustomModule { nomFake(no.nav.aap.postmottak.test.fakes.erEgenansatt ) }

        val actual = client.erEgenAnsatt(Ident("123412341243"))

        assertThat(actual).isTrue()
    }

    @Test
    fun erIkkeEgenansatt() {
        val client = NomKlient()

        val actual = client.erEgenAnsatt(Ident("123412341243"))

        assertThat(actual).isFalse()
    }

}