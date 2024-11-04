package no.nav.aap.postmottak.flyt

import no.nav.aap.WithFakes
import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovHendelseHåndterer
import no.nav.aap.postmottak.behandling.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.behandling.avklaringsbehov.LøsAvklaringsbehovBehandlingHendelse
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.behandling.avklaringsbehov.løsning.KategoriserDokumentLøsning
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
import no.nav.aap.postmottak.server.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.server.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.server.prosessering.forBehandling
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
                    .forBehandling(behandlingId).medCallId()
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
                    .forBehandling(behandlingId).medCallId()
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
        val avklaringRepository = StruktureringsvurderingRepository(connection)
        val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)

        AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
        SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
        KategorivurderingRepository(connection).lagreKategoriseringVurdering(behandlingId, Brevkode.SØKNAD)
        avklaringRepository.lagreStrukturertDokument(
            behandlingId,
            """{"søknadsDato":"2024-09-02T22:00:00.000Z","yrkesSkade":"nei","erStudent":"Nei"}"""
        )
        return behandlingId
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val journalpostId = JournalpostId(1L)
        dataSource.transaction { connection ->
            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            AvklarTemaRepository(connection).lagreTeamAvklaring(behandlingId, true)
            SaksnummerRepository(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(behandlingId).medCallId()
            )
            behandlingId
        }

        val behandling = await {
            dataSource.transaction { connection ->
                val behandlingRepository = BehandlingRepositoryImpl(connection)
                val behandling = behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
                    .find { it.typeBehandling == TypeBehandling.DokumentHåndtering }!!
                val avklaringsbehovene = hentAvklaringsbehov(behandling.id, connection)
                assertThat(
                    avklaringsbehovene.alle()
                        .firstOrNull { it.definisjon == Definisjon.KATEGORISER_DOKUMENT }).isNotNull()
                AvklaringsbehovHendelseHåndterer(connection).håndtere(
                    key = behandling.id,
                    hendelse = LøsAvklaringsbehovBehandlingHendelse(
                        KategoriserDokumentLøsning(Brevkode.SØKNAD),
                        false,
                        1,
                        Bruker("sdfgsdfg")
                    )
                )
                behandling
            }
        }

        await {
            dataSource.transaction { connection ->
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


        dataSource.transaction { connection ->
            val avklaringsbehov = hentAvklaringsbehov(behandling.id, connection)
            assertThat(avklaringsbehov.alle())
                .anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT) }
        }

        Thread.sleep(50)

        dataSource.transaction { connection ->
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
