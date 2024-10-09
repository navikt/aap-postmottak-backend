package no.nav.aap.postmottak

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepository
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.concurrent.thread

class BehandlingRepositoryImplTest {


    @BeforeEach
    fun clean() {
        InitTestDatabase.dataSource.transaction {
            it.execute("""TRUNCATE BEHANDLING CASCADE""")
        }
    }

    @Test
    fun opprettBehandling() {
        InitTestDatabase.dataSource.transaction {
            val repository = BehandlingRepositoryImpl(it)

            val journalpostId = JournalpostId(11111)
            val behandlingId = repository.opprettBehandling(journalpostId)

            assertThat(behandlingId).isNotNull
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling`() {
        inContext {
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))

            val hentetBehandling = behandlingRepository.hent(behandlingId)

            assertThat(hentetBehandling).isNotNull()
        }
    }

    @Test
    fun `hent behandling med journalpostId returnerer behandling`() {
        inContext {
            val journalpostId = JournalpostId(11111)
            behandlingRepository.opprettBehandling(journalpostId)

            val hentetBehandling = behandlingRepository.hent(journalpostId)

            assertThat(hentetBehandling).isNotNull()
        }
    }

    @Test
    fun `hent behandling med id returnerer behandling og alle vurderinger`() {
        inContext {

            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))
            avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            avklaringRepository.lagreTeamAvklaring(behandlingId, false)

            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            val behandling = dokumentbehandlingRepository.hent(behandlingId)

            assertThat(behandling.harBlittStrukturert()).isTrue()
            assertThat(behandling.harBlittKategorisert()).isTrue()
            assertThat(behandling.harTemaBlittAvklart()).isTrue()

        }
    }

    @Test
    fun `hent behandling referanse returnerer behandling og alle vurderinger`() {
        inContext {

            val behandlingsreferanse = JournalpostId(11111)
            val behandlingId = behandlingRepository.opprettBehandling(behandlingsreferanse)

            avklaringRepository.lagreStrukturertDokument(behandlingId, """{"Test: Dokument"}""")
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
            avklaringRepository.lagreTeamAvklaring(behandlingId, false)


            val hentetBehandling = dokumentbehandlingRepository.hent(behandlingsreferanse)

            assertThat(hentetBehandling.harBlittStrukturert()).isTrue()
            assertThat(hentetBehandling.harBlittKategorisert()).isTrue()
            assertThat(hentetBehandling.harTemaBlittAvklart()).isTrue()

        }
    }

    @Test
    fun `forventer at vurderingstabeller blir låst når behandlingen er låst`() {
        val behandlingId = inContext { behandlingRepository.opprettBehandling(JournalpostId(1)) }
        thread {
            inContext {
                dokumentbehandlingRepository.hent(BehandlingId(1))
                Thread.sleep(500)
                avklaringRepository.lagreTeamAvklaring(behandlingId, false)
            }
        }
        thread {
            Thread.sleep(100)
            inContext { avklaringRepository.lagreTeamAvklaring(behandlingId, true) }
        }.join()

        inContext {
            val temavurdering =
                dokumentbehandlingRepository.hent(behandlingId).vurderinger.avklarTemaVurdering?.avklaring
            assertThat(temavurdering).isNotNull().isEqualTo(true)
        }

    }

    private class Context(
        val dokumentbehandlingRepository: BehandlingRepository,
        val avklaringRepository: AvklaringRepository,
        val behandlingRepository: BehandlingRepository,
    )

    private fun <T> inContext(block: Context.() -> T): T {
        return InitTestDatabase.dataSource.transaction {
            val context =
                Context(BehandlingRepositoryImpl(it), AvklaringRepositoryImpl(it), BehandlingRepositoryImpl(it))
            context.let(block)
        }
    }
}