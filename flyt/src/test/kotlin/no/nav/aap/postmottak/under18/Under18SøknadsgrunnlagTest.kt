package no.nav.aap.postmottak.under18

import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.fakes.IsolertUnder18Repository
import no.nav.aap.postmottak.test.fakes.TestJournalPost
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.api.Test

class Under18SøknadsgrunnlagTest {
    private val repository = IsolertUnder18Repository()
    private val id = BehandlingId(1)
    private val søknad = JournalpostId(100)
    private val grunnlag = Under18Søknadsgrunnlag(1, setOf(søknad))

    @Test
    fun `avviser en registrert søknad som nå tilhører en annen person`() {
        val journalpost = TestJournalPost(journalpostId = søknad.referanse).tilJournalpost()
        val annenSøker = Under18Søknadsgrunnlag(journalpost.person.id + 1, setOf(søknad))
        assertThatIllegalStateException().isThrownBy { annenSøker.sjekkEier(journalpost) }
            .withMessageContaining("tilhører ikke lenger søkeren")
    }

    @Test
    fun `en registrert søknad kan ikke flyttes til en annen behandling`() {
        repository.lagreSøknadsgrunnlag(id, grunnlag)
        assertThatIllegalStateException().isThrownBy { repository.lagreSøknadsgrunnlag(BehandlingId(2), grunnlag) }
        assertThat(repository.hentBehandlingForSøknad(søknad)).isEqualTo(id)
    }

    @Test
    fun `en ny søknad kan ikke overskrive en annen som er lagt til i mellomtiden`() {
        repository.lagreSøknadsgrunnlag(id, grunnlag)
        val oppdatert = grunnlag.leggTilSøknad(JournalpostId(101))
        repository.lagreSøknadsgrunnlag(id, oppdatert)
        assertThatIllegalStateException().isThrownBy {
            repository.lagreSøknadsgrunnlag(id, grunnlag.leggTilSøknad(JournalpostId(102)))
        }
        assertThat(repository.hentSøknadsgrunnlag(id)).isEqualTo(oppdatert)
    }
}
