package no.nav.aap.postmottak.flyt

import no.nav.aap.WithFakes
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.kontrakt.journalpost.Status
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.sak.Saksnummer
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.server.prosessering.ProsesseringsJobber
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class Flyttest: WithFakes {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            motor.start()
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

    @AfterEach
    fun afterEach() {
        WithFakes.fakes.behandlkingsflyt.clean()
        dataSource.transaction { it.execute("""
            TRUNCATE BEHANDLING CASCADE
        """.trimIndent()) }
    }

    @Test
    fun `kjører en manuell søknad igjennom hele flyten`() {
        val behandlingId = dataSource.transaction { connection ->
            val behandlingId = opprettManuellBehandlingMedAlleAvklaringer(connection)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandlingId.toLong()).medCallId()
            )
            behandlingId
        }

        Thread.sleep(500)

        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.hent(behandlingId)

            assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        }
    }

    @Test
    fun `når det avsluttende steget feiler skal behandlingen fortsatt være åpen`() {
        val behandlingId = dataSource.transaction { connection ->
            val behandlingId = opprettManuellBehandlingMedAlleAvklaringer(connection)

            WithFakes.fakes.behandlkingsflyt.throwException(path = "soknad/send")

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandlingId.toLong()).medCallId()
            )
            behandlingId
        }

        Thread.sleep(500)

        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.hent(behandlingId)

            assertThat(behandling.status()).isNotEqualTo(Status.AVSLUTTET)
        }
    }

    private fun opprettManuellBehandlingMedAlleAvklaringer(connection: DBConnection): BehandlingId {
        val behandlingRepository = BehandlingRepositoryImpl(connection)
        val avklaringRepository = AvklaringRepositoryImpl(connection)
        val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1)).id

        avklaringRepository.lagreTeamAvklaring(behandlingId, true)
        avklaringRepository.lagreSakVurdering(behandlingId, Saksnummer("23452345"))
        avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
        avklaringRepository.lagreStrukturertDokument(behandlingId, """{"søknadsDato":"2024-09-02T22:00:00.000Z","yrkesSkade":"nei","erStudent":"Nei"}""")
        return behandlingId
    }

}
