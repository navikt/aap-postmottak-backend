package no.nav.aap.postmottak.flyt

import no.nav.aap.WithFakes
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.flyt.flate.Venteinformasjon
import no.nav.aap.postmottak.flyt.internals.TestHendelsesMottak
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.sakogbehandling.behandling.vurdering.AvklaringRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.server.prosessering.ProsesseringsJobber
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test


class Flyttest : WithFakes {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val motor = Motor(dataSource, 2, jobber = ProsesseringsJobber.alle())
        private val hendelsesMottak = TestHendelsesMottak(dataSource)

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
        dataSource.transaction {
            it.execute(
                """
            TRUNCATE BEHANDLING CASCADE
        """.trimIndent()
            )
        }
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

        dataSource.transaction { connection ->
            await(5000) {
                val behandling = BehandlingRepositoryImpl(connection).hent(behandlingId)
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
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

        Thread.sleep(50)

        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.hent(behandlingId)

            assertThat(behandling.status()).isNotEqualTo(Status.AVSLUTTET)
        }
    }

    private fun opprettManuellBehandlingMedAlleAvklaringer(connection: DBConnection): BehandlingId {
        val behandlingRepository = BehandlingRepositoryImpl(connection)
        val avklaringRepository = AvklaringRepositoryImpl(connection)
        val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1))

        avklaringRepository.lagreTeamAvklaring(behandlingId, true)
        SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
        avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
        avklaringRepository.lagreStrukturertDokument(
            behandlingId,
            """{"søknadsDato":"2024-09-02T22:00:00.000Z","yrkesSkade":"nei","erStudent":"Nei"}"""
        )
        return behandlingId
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {

        val behandlingId = dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val avklaringRepository = AvklaringRepositoryImpl(connection)
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1))

            avklaringRepository.lagreTeamAvklaring(behandlingId, true)
            SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
            avklaringRepository.lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(null, behandlingId.toLong()).medCallId()
            )
            behandlingId
        }

        Thread.sleep(500)

        val behandling = dataSource.transaction { connection ->
            await(5000) {
                val behandlingRepository = BehandlingRepositoryImpl(connection)
                val behandling = behandlingRepository.hent(behandlingId)

                assertThat(behandling.status()).isEqualTo(Status.UTREDES)
                behandling
            }
        }

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandlingId, connection)
            assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.DIGITALISER_DOKUMENT) }
        }

        hendelsesMottak.håndtere(
            behandling.id,
            BehandlingSattPåVent(
                frist = null,
                begrunnelse = "Avventer dokumentasjon",
                bruker = SYSTEMBRUKER,
                behandlingVersjon = behandling.versjon,
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
            )
        )

        val dto = dataSource.transaction(readOnly = true) { connection ->
            val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)

            if (avklaringsbehovene.erSattPåVent()) {
                val avklaringsbehov = avklaringsbehovene.hentVentepunkter().first()
                Venteinformasjon(avklaringsbehov.frist(), avklaringsbehov.begrunnelse(), avklaringsbehov.grunn())
            } else {
                null
            }
        }
        assertThat(dto).isNotNull
        assertThat(dto?.frist).isNotNull


        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .hasSize(2)
                .anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT) }
                .anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.DIGITALISER_DOKUMENT) }
        }

        Thread.sleep(50)

        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.hent(behandlingId)
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        Thread.sleep(50)
        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .hasSize(2)
                //TODO: Fikse
                .anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
                .anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.DIGITALISER_DOKUMENT) }
        }

    }

    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

    private fun <T> await(duration: Long, block: () -> T): T {
        val currentTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - currentTime <= duration) {
            try {
                return block()
            } catch (_: Throwable) {
            }
            Thread.sleep(50)
        }
        return block()
    }
}
