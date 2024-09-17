package no.nav.aap.behandlingsflyt.flyt

import no.nav.aap.WithFakes
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.behandlingsflyt.server.prosessering.ProsesseringsJobber
import no.nav.aap.behandlingsflyt.test.Fakes
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.verdityper.sakogbehandling.Status
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.AfterAll
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

    @Test
    fun `kjører en manuell søknad igjennom hele flyten`() {
        val behandlingId = dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.opprettBehandling(JournalpostId(1))

            behandlingRepository.lagreTeamAvklaring(behandling.id, true)
            behandlingRepository.lagreKategoriseringVurdering(behandling.id, Brevkode.SØKNAD)
            behandlingRepository.lagreStrukturertDokument(behandling.id, """{"yolo": "swag"}""")

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandling.id.toLong()).medCallId()
            )
            behandling.id
        }

        Thread.sleep(500)

        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.hent(behandlingId)

            assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
        }
    }

}
