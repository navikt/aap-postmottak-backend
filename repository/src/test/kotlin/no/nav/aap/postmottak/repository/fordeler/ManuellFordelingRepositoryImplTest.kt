package no.nav.aap.postmottak.repository.fordeler

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.Fagsystem
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AutoClose
import org.junit.jupiter.api.Test

class ManuellFordelingRepositoryImplTest {

    @AutoClose
    private val dataSource = TestDataSource()

    @Test
    fun `lesing fungerer`() {
        dataSource.transaction {
            it.execute(
                """
                insert into manuell_fordeling (ident, fagsystem) values 
                ('11111111111', 'kelvin'),
                ('22222222222', 'arena')
            """
            )

            val manuellFordelingRepositoryImpl = ManuellFordelingRepositoryImpl(it)

            assertThat(manuellFordelingRepositoryImpl.fordelTilFagsystem(Ident("1".repeat(11))))
                .isEqualTo(Fagsystem.kelvin)

            assertThat(manuellFordelingRepositoryImpl.fordelTilFagsystem(Ident("2".repeat(11))))
                .isEqualTo(Fagsystem.arena)

            assertThat(manuellFordelingRepositoryImpl.fordelTilFagsystem(Ident("3".repeat(11))))
                .isNull()
        }
    }
}