package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class AvklarTemaRepositoryTest {

    private val dataSource = InitTestDatabase.dataSource

    @AfterEach
    fun afterEach() {
        dataSource.transaction { it.execute("TRUNCATE behandling CASCADE ") }
    }

    @Test
    fun `når teamavklaring blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))

            avklarTemaRepository.lagreTeamAvklaring(behandlingId, false)

            assertThat(avklarTemaRepository.hentTemaAvklaring(behandlingId)?.skalTilAap).isFalse()
        }
    }

    @Test
    fun `når to temaavklaringer blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklarTemaRepository.lagreTeamAvklaring(behandlingId, false) }
        inContext { avklarTemaRepository.lagreTeamAvklaring(behandlingId, true) }
        inContext {
            assertThat(avklarTemaRepository.hentTemaAvklaring(behandlingId)?.skalTilAap).isTrue()
        }
    }

    @Test
    fun `kan ikke ha to aktive vurderinger på samme behandling`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklarTemaRepository.lagreTeamAvklaring(behandlingId, false) }

        catchThrowable {
            dataSource.transaction {
                it.execute(
                    """insert INTO AVKLARTEMA_GRUNNLAG (behandling_Id, TEMAVURDERING_ID) 
             SELECT ?, id FROM TEMAVURDERING LIMIT 1""".trimMargin()
                ) { setParams { setLong(1, behandlingId.toLong()) } }
            }
        }
    }

    @Test
    fun `hvis to vurderinger blir lagt på samme sak blir den første deaktivert`() {
        val saksnummer = "234234"
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        inContext { avklarTemaRepository.lagreTeamAvklaring(behandlingId, false) }
        inContext { avklarTemaRepository.lagreTeamAvklaring(behandlingId, true) }
    }


    private class Context(
        val behandlingRepository: BehandlingRepository,
        val avklarTemaRepository: AvklarTemaRepository
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklarTemaRepository(it))
            context.let(block)
        }
    }

}