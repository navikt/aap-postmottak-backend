package no.nav.aap.postmottak.klient

import no.nav.aap.api.intern.Kilde
import no.nav.aap.api.intern.Periode
import no.nav.aap.api.intern.SakStatus
import no.nav.aap.api.intern.Status
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.test.fakes.aapInternApiFake
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.*

class AapApiInternKlientTest : WithFakes {
    @AfterEach
    fun tearDown() {
        WithFakes.fakes.aapInternApi.clean()
    }

    val apiIntern = AapInternApiKlient()

    @Test
    fun `Kan parse AAP-saker`() {
        WithFakes.fakes.aapInternApi.setCustomModule {
            aapInternApiFake(
                sakerRespons = """
             [
                {
                    "sakId": "1234",
                    "statusKode": "AVSLU",
                    "periode": {
                        "fraOgMedDato": "2020-01-01",
                        "tilOgMedDato": "2020-12-31"
                    },
                    "kilde": "ARENA"
                }
            ]
            """.trimIndent()
            )
        }

        val res = apiIntern.hentAapSakerForPerson(Person(1, UUID.randomUUID(), emptyList()))

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