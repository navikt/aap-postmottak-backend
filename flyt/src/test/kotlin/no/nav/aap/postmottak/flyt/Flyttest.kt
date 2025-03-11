package no.nav.aap.postmottak.flyt

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.aap.WithDependencies
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.type.Periode
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.Motor
import no.nav.aap.motor.testutil.TestUtil
import no.nav.aap.postmottak.PrometheusProvider
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.api.flyt.Venteinformasjon
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.Digitaliseringsvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.digitalisering.DigitaliseringsvurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksinfo
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tema.Tema
import no.nav.aap.postmottak.flyt.internals.TestHendelsesMottak
import no.nav.aap.postmottak.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.FordelingRegelJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesserBehandlingJobbUtfører
import no.nav.aap.postmottak.prosessering.ProsesseringsJobber
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.ANNET_TEMA
import no.nav.aap.postmottak.test.fakes.DIGITAL_SØKNAD_ID
import no.nav.aap.postmottak.test.fakes.LEGEERKLÆRING
import no.nav.aap.postmottak.test.fakes.LEGEERKLÆRING_IKKE_TIL_KELVIN
import no.nav.aap.postmottak.test.fakes.STATUS_JOURNALFØRT
import no.nav.aap.postmottak.test.fakes.STATUS_JOURNALFØRT_ANNET_FAGSYSTEM
import no.nav.aap.postmottak.test.fakes.UGYLDIG_STATUS
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.LocalDate


@Fakes
class Flyttest : WithDependencies {
    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val hendelsesMottak = TestHendelsesMottak(dataSource)
        private val motor = Motor(InitTestDatabase.dataSource, 2, jobber = ProsesseringsJobber.alle())
        private val util =
            TestUtil(dataSource, ProsesseringsJobber.alle().filter { it.cron() != null }.map { it.type() })


        @JvmStatic
        @BeforeAll
        fun beforeAll() {
            motor.start()
            PrometheusProvider.prometheus = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
        }

        @AfterAll
        @JvmStatic
        internal fun afterAll() {
            motor.stop()
        }
    }

    @AfterEach
    fun afterEach() {
        dataSource.transaction {
            it.execute(
                """
            TRUNCATE BEHANDLING CASCADE;
            TRUNCATE innkommende_journalpost CASCADE;
            TRUNCATE regel_evaluering CASCADE;
        """.trimIndent()
            )
        }
    }

    @Test
    fun fordel() {
        val journalpostId = DIGITAL_SØKNAD_ID
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører).medJournalpostId(journalpostId)
            )
        }
        util.ventPåSvar()
        val behandlinger = dataSource.transaction(readOnly = true) {
            BehandlingRepositoryImpl(it).hentAlleBehandlingerForSak(journalpostId)
        }
        assertThat(behandlinger).isNotEmpty
    }

    @Test
    fun `Helautomatisk flyt for legeerklæring som ikke skal til Kelvin`() {
        val journalpostId = LEGEERKLÆRING_IKKE_TIL_KELVIN
        val behandlingId = dataSource.transaction { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
            behandlingId
        }

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        dataSource.transaction(readOnly = true) { connection ->
            val behandlinger = BehandlingRepositoryImpl(connection).hentAlleBehandlingerForSak(journalpostId)
            assertThat(behandlinger).hasSize(1)
            assertThat(
                behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
                1
            )

        }
    }

    @Test
    fun `Helautomatisk flyt for digital legeerklæring som skal til Kelvin`() {
        val journalpostId = LEGEERKLÆRING
        val behandlingId = dataSource.transaction { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }

        dataSource.transaction { connection ->
            SaksnummerRepositoryImpl(connection).lagreKelvinSak(
                behandlingId, listOf(
                    Saksinfo(
                        "sak: 1", Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2022, 1, 31)),
                    )
                )
            )
        }
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
            behandlingId
        }

        util.ventPåSvar()
        val behandlinger = dataSource.transaction(readOnly = true) { connection ->
            BehandlingRepositoryImpl(connection).hentAlleBehandlingerForSak(journalpostId)
        }
        assertThat(behandlinger).hasSize(2)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.DokumentHåndtering && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )
    }

    @Test
    fun `Flyt for journalpost som er blitt behandlet i gosys`() {
        val (journalpostId, behandlingId) = dataSource.transaction { connection ->
            val journalpostId = ANNET_TEMA
            val behandlingId = RepositoryProvider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            Pair(journalpostId, behandlingId)
        }
        dataSource.transaction { connection ->
            val repositoryProvider = RepositoryProvider(connection)
            repositoryProvider.provide(AvklarTemaRepository::class).lagreTemaAvklaring(behandlingId, false, Tema.UKJENT)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }

        util.ventPåSvar()
        val behandling = dataSource.transaction(readOnly = true) { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
        }
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val jobber: List<String> = dataSource.transaction { connection ->
            connection.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                setRowMapper { row -> row.getString("type") }
            }
        }

        assertThat(jobber).hasSize(0)

    }


    @Test
    fun `Full helautomatisk flyt for søknad`() {
        val journalpostId = DIGITAL_SØKNAD_ID
        val behandlingId = dataSource.transaction {
            RepositoryProvider(it).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
            behandlingId
        }

        util.ventPåSvar()
        val behandlinger = dataSource.transaction(readOnly = true) {
            RepositoryProvider(it).provide(BehandlingRepository::class).hentAlleBehandlingerForSak(journalpostId)
        }

        assertThat(behandlinger).allMatch { it.status() == Status.AVSLUTTET }
    }

    @Test
    fun `kjører en manuell søknad igjennom hele flyten`() {
        val (behandlingId, journalpostId) = dataSource.transaction { connection ->
            val journalpostId = JournalpostId(1)
            Pair(opprettManuellBehandlingMedAlleAvklaringer(connection, journalpostId), journalpostId)
        }

        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
            behandlingId
        }

        util.ventPåSvar()
        val behandling = dataSource.transaction(readOnly = true) { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
        }
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
    }

    @Test
    fun `Forventer at en fordelerjobb oppretter en journalføringsbehandling`() {
        val journalpostId = JournalpostId(1L)

        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(FordelingRegelJobbUtfører).forSak(journalpostId.referanse).medJournalpostId(journalpostId)
                    .medCallId()
            )
        }

        util.ventPåSvar()
        val behandling = dataSource.transaction(readOnly = true) { connection ->
            val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
            behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
                .find { it.typeBehandling == TypeBehandling.Journalføring }!!
        }

        assertNotNull(behandling)
    }

    @Test
    fun `Blir satt på vent for etterspørring av informasjon`() {
        val journalpostId = JournalpostId(2L)
        val behandlingId = dataSource.transaction { connection ->
            val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
            behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }

        dataSource.transaction { connection ->
            AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
            SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
        }
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        var behandling = dataSource.transaction(readOnly = true) { connection ->
            val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
            val behandling = behandlingRepository.hentAlleBehandlingerForSak(journalpostId)
                .find { it.typeBehandling == TypeBehandling.DokumentHåndtering }!!
            behandling
        }

        util.ventPåSvar()
        val alleAvklaringsbehov = dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).alle()
        }
        assertThat(alleAvklaringsbehov).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.DIGITALISER_DOKUMENT) }

        hendelsesMottak.håndtere(
            behandling.id, BehandlingSattPåVent(
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


        val alleAvklaringsbehov2 = dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).alle()
        }
        assertThat(alleAvklaringsbehov2).anySatisfy { assertTrue(it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT) }


        behandling = dataSource.transaction(readOnly = true) { connection ->
            val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
            behandlingRepository.hent(behandling.id)
        }
        assertThat(behandling.status()).isEqualTo(Status.UTREDES)

        val alleAvklaringsbehov3 = dataSource.transaction(readOnly = true) { connection ->
            hentAvklaringsbehov(behandling.id, connection).alle()
        }

        assertThat(alleAvklaringsbehov3).anySatisfy { !it.erÅpent() && it.definisjon == Definisjon.MANUELT_SATT_PÅ_VENT }

    }

    @Test
    fun `Skal ikke opprette dokumentflyt dersom journalposten har ugyldig status`() {
        val (behandlingId, journalpostId) = dataSource.transaction { connection ->
            val journalpostId = UGYLDIG_STATUS
            val behandlingId = RepositoryProvider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            Pair(behandlingId, journalpostId)
        }
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }

        util.ventPåSvar()

        val behandling = dataSource.transaction(readOnly = true) { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
        }
        assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

        val jobber: List<String> = dataSource.transaction {
            it.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                setRowMapper { row -> row.getString("type") }
            }
        }
        assertThat(jobber).hasSize(0)


    }

    @Test
    fun `Skal videresende dersom journalposten ble journalført utenfor postmottak med tema AAP på Kelvin fagsak `() {
        val journalpostId = STATUS_JOURNALFØRT
        val behandlingId = dataSource.transaction { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        val behandlinger = dataSource.transaction(readOnly = true) { connection ->
            BehandlingRepositoryImpl(connection).hentAlleBehandlingerForSak(journalpostId)
        }
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )

    }

    @Test
    fun `Skal ikke videresende dersom journalposten ble journalført utenfor postmottak med tema AAP, men på annet fagsystem`() {
        val journalpostId = STATUS_JOURNALFØRT_ANNET_FAGSYSTEM
        val behandlingId = dataSource.transaction { connection ->
            RepositoryProvider(connection).provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        }
        dataSource.transaction { connection ->
            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører).forBehandling(journalpostId.referanse, behandlingId.id)
                    .medCallId()
            )
        }

        util.ventPåSvar(journalpostId.referanse, behandlingId.id)

        val behandlinger = dataSource.transaction(readOnly = true) { connection ->
            BehandlingRepositoryImpl(connection).hentAlleBehandlingerForSak(journalpostId)
        }
        assertThat(behandlinger).hasSize(1)
        assertThat(
            behandlinger.filter { it.typeBehandling == TypeBehandling.Journalføring && it.status() == Status.AVSLUTTET }).hasSize(
            1
        )

    }


    private fun opprettManuellBehandlingMedAlleAvklaringer(
        connection: DBConnection, journalpostId: JournalpostId
    ): BehandlingId {
        val repositoryProvider = RepositoryProvider(connection)
        val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
        val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

        repositoryProvider.provide(AvklarTemaRepository::class).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
        repositoryProvider.provide(SaksnummerRepository::class)
            .lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
        repositoryProvider.provide(DigitaliseringsvurderingRepository::class).lagre(
            behandlingId, Digitaliseringsvurdering(
                InnsendingType.SØKNAD,
                """{"søknadsDato":"2024-09-02T22:00:00.000Z", "yrkesskade":"nei", "student": {"erStudent":"Nei"}}""",
                LocalDate.of(2024, 9, 2)
            )
        )
        return behandlingId
    }

    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

}
