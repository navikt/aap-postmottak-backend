package no.nav.aap.behandlingsflyt

import no.nav.aap.behandlingsflyt.dbconnect.transaction
import no.nav.aap.behandlingsflyt.dbtest.InitTestDatabase
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.PersonRepository
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.sakRepository
import no.nav.aap.verdityper.Periode
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class SakRepositoryTest {

    @Test
    fun `skal avklare yrkesskade hvis det finnes spor av yrkesskade`() {
        InitTestDatabase.dataSource.transaction { connection ->
            val personRepository = PersonRepository(connection)
            val sakRepository = sakRepository(connection)

            val person = personRepository.finnEllerOpprett(Ident("23067823253"))

            val sak = sakRepository.finnEllerOpprett(
                person,
                Periode(
                    LocalDate.now().minusMonths(3),
                    LocalDate.now().plusYears(1)
                )
            )

            val alleSaker = sakRepository.finnAlle()
            assertThat(alleSaker).isNotEmpty

            assertThat(alleSaker).contains(sak)
        }
    }
}
