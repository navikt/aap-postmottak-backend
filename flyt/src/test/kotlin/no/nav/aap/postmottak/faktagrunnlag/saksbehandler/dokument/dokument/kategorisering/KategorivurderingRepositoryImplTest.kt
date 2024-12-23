package no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.dokument.kategorisering

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.KategorivurderingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KategorivurderingRepositoryImplTest {

    @BeforeEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
        
        RepositoryRegistry.register<KategorivurderingRepositoryImpl>()
            .register<BehandlingRepositoryImpl>()
    }

    @Test
    fun `når kategoriseringvurdering blir lagret forventer jeg å finne den på behandlngen`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(
                JournalpostId(
                    11111
                ), TypeBehandling.Journalføring
            )

            kategorivurderingRepository.lagreKategoriseringVurdering(behandlingId, InnsendingType.SØKNAD)

            val vurdering = kategorivurderingRepository.hentKategoriAvklaring(behandlingId)

            assertThat(vurdering?.avklaring).isEqualTo(InnsendingType.SØKNAD)
        }
    }


    @Test
    fun `når to kategoriseringvurderinger blir lagret forventer jeg å finne den siste på behandlingen`() {
        val behandlingId =
            inContext { behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring) }
        inContext { kategorivurderingRepository.lagreKategoriseringVurdering(behandlingId, InnsendingType.SØKNAD) }
        Thread.sleep(100)
        inContext { kategorivurderingRepository.lagreKategoriseringVurdering(behandlingId, InnsendingType.PLIKTKORT) }
        inContext {
            val vudering = kategorivurderingRepository.hentKategoriAvklaring(behandlingId)

            assertThat(vudering?.avklaring).isEqualTo(InnsendingType.PLIKTKORT)
        }
    }

    @Test
    fun `kan kopiere vurdering fra en behnadling til en annen`() {
        val journalpostId = JournalpostId(1)
        inContext {
            val fraBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val tilBehandling = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.DokumentHåndtering)
            kategorivurderingRepository.lagreKategoriseringVurdering(fraBehandling, InnsendingType.SØKNAD)
            kategorivurderingRepository.kopier(fraBehandling, tilBehandling)

            assertThat(kategorivurderingRepository.hentKategoriAvklaring(tilBehandling)?.avklaring).isEqualTo(
                InnsendingType.SØKNAD
            )
        }
    }

    private class Context(
        val kategorivurderingRepository: KategoriVurderingRepository,
        val behandlingRepository: BehandlingRepository
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val repositoryProvider = RepositoryProvider(it)
            val context = Context(
                repositoryProvider.provide(KategorivurderingRepositoryImpl::class),
                repositoryProvider.provide(BehandlingRepositoryImpl::class)
            )
            context.let(block)
        }
    }

}