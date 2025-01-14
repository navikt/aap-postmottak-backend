package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class AvklarTemaRepositoryImplTest {

    private val dataSource = InitTestDatabase.dataSource

    @AfterEach
    fun afterEach() {
        dataSource.transaction { it.execute("TRUNCATE behandling CASCADE ") }
    }

    @Test
    fun `når teamavklaring blir lagret forventer jeg å finne den på behandlingen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)

            avklarTemaRepository.lagreTemaAvklaring(behandlingId, false)

            assertThat(avklarTemaRepository.hentTemaAvklaring(behandlingId)?.skalTilAap).isFalse()
        }
    }

    @Test
    fun `når to temaavklaringer blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { avklarTemaRepository.lagreTemaAvklaring(behandlingId, false) }
        inContext { avklarTemaRepository.lagreTemaAvklaring(behandlingId, true) }
        inContext {
            assertThat(avklarTemaRepository.hentTemaAvklaring(behandlingId)?.skalTilAap).isTrue()
        }
    }

    @Test
    fun `kan ikke ha to aktive vurderinger på samme behandling`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { avklarTemaRepository.lagreTemaAvklaring(behandlingId, false) }

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
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { avklarTemaRepository.lagreTemaAvklaring(behandlingId, false) }
        inContext { avklarTemaRepository.lagreTemaAvklaring(behandlingId, true) }
    }

    @Test
    fun `kan kopiere vurdering fra en behnadling til en annen`() {
        val journalpostId = JournalpostId(1)
        inContext {
            val fraBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val tilBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering)
            avklarTemaRepository.lagreTemaAvklaring(fraBehandling, true)
            avklarTemaRepository.kopier(fraBehandling, tilBehandling)

            assertThat(avklarTemaRepository.hentTemaAvklaring(tilBehandling)?.skalTilAap).isTrue()
        }
    }

    private class Context(
        val behandlingRepository: BehandlingRepository,
        val avklarTemaRepository: AvklarTemaRepository
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return dataSource.transaction {
            val context = Context(BehandlingRepositoryImpl(it), AvklarTemaRepositoryImpl(it))
            context.let(block)
        }
    }

}