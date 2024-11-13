package no.nav.aap.postmottak.fordeler

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class HendelsesRepositoryTest {

    val dataSource = InitTestDatabase.dataSource

    @Test
    fun `kan lagre og hente joarkhendelser`() {

        val joarkHendelse = JoarkHendelse("NØKKEL", "VERDI")

        dataSource.transaction { connection ->
            val hendelsesRepository = HendelsesRepository(connection)

            hendelsesRepository.lagreHendelse(joarkHendelse)

            val actual = hendelsesRepository.hentHendelse(joarkHendelse.hendelsesid)

            assertThat(actual).isEqualTo(joarkHendelse)
        }

    }

}