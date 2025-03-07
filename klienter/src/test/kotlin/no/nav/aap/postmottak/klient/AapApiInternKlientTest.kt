package no.nav.aap.postmottak.klient

import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.intern.Status
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.IDENT_MED_SAK_I_ARENA
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

// TODO: fiks
@Fakes
class AapApiInternKlientTest {
    val apiIntern = AapInternApiKlient()

    @Test
    fun `Kan parse AAP-saker`() {
        val res = apiIntern.hentAapSakerForPerson(Person(1, UUID.randomUUID(), listOf(IDENT_MED_SAK_I_ARENA)))

        assertThat(res).isEqualTo(
            listOf(
                SakStatus(
                    "1234",
                    Status.AVSLU,
                    Periode(fraOgMedDato = LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31)),
                    Kilde.ARENA
                )
            )
        )
    }
}