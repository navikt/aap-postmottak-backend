package no.nav.aap.postmottak.flyt

import io.ktor.http.*
import io.ktor.server.response.*
import no.nav.aap.postmottak.test.fakes.WithFakes
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
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklarteam.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategorivurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
import no.nav.aap.postmottak.flyt.flate.Venteinformasjon
import no.nav.aap.postmottak.flyt.internals.TestHendelsesMottak
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.sakogbehandling.behandling.dokumenter.Brevkode
import no.nav.aap.postmottak.server.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.server.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.server.prosessering.medJournalpostId
import no.nav.aap.postmottak.test.fakes.DIGITAL_SØKNAD_ID
import no.nav.aap.postmottak.test.fakes.behandlingsflytFake
import no.nav.aap.verdityper.sakogbehandling.BehandlingId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
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
    fun `Full helautomatisk flyt`() {
        val journalpostId = DIGITAL_SØKNAD_ID
        dataSource.transaction {
            val behandlingId =
                BehandlingRepositoryImpl(it).opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            FlytJobbRepository(it).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await(10000) {
            dataSource.transaction(readOnly = true) {
                val behandlinger = BehandlingRepositoryImpl(it).hentAlleBehandlingerForSak(journalpostId)
                assertThat(behandlinger).allMatch { it.status() == Status.AVSLUTTET }
            }
        }
    }

    @Test
    fun `kjører en manuell søknad igjennom hele flyten`() {
        val behandlingId = dataSource.transaction { connection ->
            val journalpostId = JournalpostId(1)
            val behandlingId = opprettManuellBehandlingMedAlleAvklaringer(connection, journalpostId)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandling = BehandlingRepositoryImpl(connection).hent(behandlingId)
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
        }
    }

    @Test
    fun `når det avsluttende steget feiler skal behandlingen fortsatt være åpen`() {
        val journalpostId = JournalpostId(1)
        dataSource.transaction { connection ->
            val behandlingId = opprettManuellBehandlingMedAlleAvklaringer(connection, journalpostId)

            WithFakes.fakes.behandlkingsflyt.setCustomModule {
                behandlingsflytFake(send = {
                    suspend {
                        call.respond(
                            HttpStatusCode.BadRequest,
                            "NOPE"
                        )
                    }
                })
            }

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandlingRepository = BehandlingRepositoryImpl(connection)
                val behandlinger = behandlingRepository.hentAlleBehandlingerForSak(journalpostId)

                assertThat(behandlinger).anyMatch { it.typeBehandling == TypeBehandling.DokumentHåndtering && it.status() == Status.IVERKSETTES }
            }
        }
    }

    @Test
    fun `Forventer at en fordelerjobb oppretter en journalføringsbehandling`() {
        val journalpostId = JournalpostId(1L)

        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(FordelingRegelJobbUtfører)
                    .medJournalpostId(journalpostId).medCallId()
            )
        }

        val behandling = await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandlingRepository = BehandlingRepositoryImpl(connection)
                behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
                    .find { it.typeBehandling == TypeBehandling.Journalføring }!!

            }
        }

        assertNotNull(behandling)
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val journalpostId = JournalpostId(1L)
        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
            SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
            KategorivurderingRepository(connection).lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        val behandling = await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandlingRepository = BehandlingRepositoryImpl(connection)
                val behandling = behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
                    .find { it.typeBehandling == TypeBehandling.DokumentHåndtering }!!
                behandling
            }
        }

        await {
            dataSource.transaction(readOnly = true) { connection ->
                val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
                assertThat(avklaringsbehov.alle()).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.DIGITALISER_DOKUMENT) }
            }
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


        dataSource.transaction(readOnly = true) { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT) }
        }

        Thread.sleep(50)

        dataSource.transaction(readOnly = true) { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandling = behandlingRepository.hent(behandling.id)
            assertThat(behandling.status()).isEqualTo(Status.UTREDES)
        }

        Thread.sleep(50)

        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }
        }

    }

    private fun opprettManuellBehandlingMedAlleAvklaringer(
        connection: DBConnection,
        journalpostId: JournalpostId
    ): BehandlingId {
        val behandlingRepository = BehandlingRepositoryImpl(connection)
        val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

        AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
        SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
        KategorivurderingRepository(connection).lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
        StruktureringsvurderingRepository(connection).lagreStrukturertDokument(
            behandlingId,
            """{"søknadsDato":"2024-09-02T22:00:00.000Z","yrkesSkade":"nei","erStudent":"Nei"}"""
        )
        return behandlingId
    }

    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

    private fun <T> await(maxWait: Long = 5000, block: () -> T): T {
        val currentTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - currentTime <= maxWait) {
            try {
                return block()
            } catch (_: Throwable) {
            }
            Thread.sleep(50)
        }
        return block()
    }
}
