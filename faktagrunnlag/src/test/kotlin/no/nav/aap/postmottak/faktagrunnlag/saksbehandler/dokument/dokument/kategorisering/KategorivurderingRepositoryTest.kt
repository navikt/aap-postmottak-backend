package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.dokument.kategorisering

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KategorivurderingRepositoryTest {

    @BeforeEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun `når kategoriseringvurdering blir lagret forventer jeg å finne den på behandlngen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)

            kategorivurderingRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val vurdering = kategorivurderingRepository.hentKategoriAvklaring(behandlingId)

            assertThat(vurdering?.avklaring).isEqualTo(Brevkode.SØKNAD)
        }
    }


    @Test
    fun `når to kategoriseringvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { kategorivurderingRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD) }
        Thread.sleep(100)
        inContext { kategorivurderingRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.PLIKTKORT) }
        inContext {
            val vudering = kategorivurderingRepository.hentKategoriAvklaring(behandlingId)

            assertThat(vudering?.avklaring).isEqualTo(Brevkode.PLIKTKORT)
        }
    }

    private class Context(
        val kategorivurderingRepository: KategorivurderingRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T>inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context = Context(KategorivurderingRepository(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }

}