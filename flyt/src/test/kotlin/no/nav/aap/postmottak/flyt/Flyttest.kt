package no.nav.aap.postmottak.flyt

import io.ktor.http.*
import io.ktor.server.response.*
import no.nav.aap.WithDependencies
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.api.flyt.Venteinformasjon
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.AvklarTemaRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.avklartema.Tema
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.finnsak.Saksvurdering
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.kategorisering.KategoriVurderingRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.strukturering.StruktureringsvurderingRepository
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
import no.nav.aap.postmottak.prosessering.medJournalpostId
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.KategorivurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.test.WithMotor
import no.nav.aap.postmottak.test.await
import no.nav.aap.postmottak.test.fakes.ANNET_TEMA
import no.nav.aap.postmottak.test.fakes.DIGITAL_SØKNAD_ID
import no.nav.aap.postmottak.test.fakes.LEGEERKLÆRING
import no.nav.aap.postmottak.test.fakes.UGYLDIG_STATUS
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.test.fakes.behandlingsflytFake
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Flyttest : WithFakes, WithDependencies, WithMotor {

    companion object {
        private val dataSource = InitTestDatabase.dataSource
        private val hendelsesMottak = TestHendelsesMottak(dataSource)

    }

    @AfterEach
    fun afterEach() {
        WithFakes.fakes.behandlingsflyt.clean()
        dataSource.transaction {
            it.execute(
                """
            TRUNCATE BEHANDLING CASCADE;
        """.trimIndent()
            )
        }
    }

    @Test
    fun fordel() {
        val journalpostId = DIGITAL_SØKNAD_ID
        dataSource.transaction {
            FlytJobbRepository(it).leggTil(
                JobbInput(FordelingRegelJobbUtfører)
                    .medJournalpostId(journalpostId)
            )
        }

        await(10000) {
            dataSource.transaction(readOnly = true) {
                dataSource.transaction(readOnly = true) {
                    val behandlinger = BehandlingRepositoryImpl(it).hentAlleBehandlingerForSak(journalpostId)
                    assertThat(behandlinger).isNotEmpty
                }
            }
        }
    }

    @Test
    fun `Flyt for legeerklæring som ikke skal til Kelvin`() {
        val behandlingId = dataSource.transaction { connection ->
            val journalpostId = LEGEERKLÆRING
            val behandlingId = RepositoryProvider(connection)
                .provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)


            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandling = RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

                val jobber: List<String> = connection.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                    setRowMapper { row -> row.getString("type") }
                }
                assertThat(jobber).hasSize(0)
            }
        }
    }

    @Test
    fun `Flyt for journalpost som er blitt behandlet i gosys`() {
        val behandlingId = dataSource.transaction { connection ->
            val journalpostId = ANNET_TEMA
            val behandlingId = RepositoryProvider(connection)
                .provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            val repositoryProvider = RepositoryProvider(connection)
            repositoryProvider.provide(AvklarTemaRepository::class).lagreTemaAvklaring(behandlingId, false, Tema.UKJENT)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandling = RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

                val jobber: List<String> = connection.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                    setRowMapper { row -> row.getString("type") }
                }
                assertThat(jobber).hasSize(0)
            }
        }
    }

    @Test
    fun `Full helautomatisk flyt`() {
        val journalpostId = DIGITAL_SØKNAD_ID
        dataSource.transaction {
            val behandlingId =
                RepositoryProvider(it).provide(BehandlingRepository::class).opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            FlytJobbRepository(it).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await(10000) {
            dataSource.transaction(readOnly = true) {
                val behandlinger = RepositoryProvider(it).provide(BehandlingRepository::class).hentAlleBehandlingerForSak(journalpostId)
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
                val behandling = RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)
            }
        }
    }

    @Test
    fun `når det avsluttende steget feiler skal behandlingen fortsatt være åpen`() {
        val journalpostId = JournalpostId(1)
        dataSource.transaction { connection ->
            val behandlingId = opprettManuellBehandlingMedAlleAvklaringer(connection, journalpostId)

            WithFakes.fakes.behandlingsflyt.setCustomModule {
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
                val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
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
                    .forSak(journalpostId.referanse)
                    .medJournalpostId(journalpostId)
                    .medCallId()
            )
        }

        val behandling = await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
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
            val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
            val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            AvklarTemaRepositoryImpl(connection).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
            SaksnummerRepositoryImpl(connection).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
            KategorivurderingRepositoryImpl(connection).lagreKategoriseringVurdering(behandlingId, InnsendingType.SØKNAD)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        val behandling = await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
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
            val behandlingRepository = RepositoryProvider(connection).provide(BehandlingRepository::class)
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
    
    @Test
    fun `Skal ikke opprette dokumentflyt dersom journalposten har ugyldig status`() {
        val behandlingId = dataSource.transaction { connection ->
            val journalpostId = UGYLDIG_STATUS
            val behandlingId = RepositoryProvider(connection)
                .provide(BehandlingRepository::class)
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)

            FlytJobbRepository(connection).leggTil(
                JobbInput(ProsesserBehandlingJobbUtfører)
                    .forBehandling(journalpostId.referanse, behandlingId.id).medCallId()
            )
            behandlingId
        }

        await {
            dataSource.transaction(readOnly = true) { connection ->
                val behandling = RepositoryProvider(connection).provide(BehandlingRepository::class).hent(behandlingId)
                assertThat(behandling.status()).isEqualTo(Status.AVSLUTTET)

                val jobber: List<String> = connection.queryList("""SELECT * FROM behandling WHERE type = 'DokumentHåndtering'""") {
                    setRowMapper { row -> row.getString("type") }
                }
                assertThat(jobber).hasSize(0)
            }
        }
    }

    private fun opprettManuellBehandlingMedAlleAvklaringer(
        connection: DBConnection,
        journalpostId: JournalpostId
    ): BehandlingId {
        val repositoryProvider = RepositoryProvider(connection)
        val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
        val behandlingId = behandlingRepository.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
        
        repositoryProvider.provide(AvklarTemaRepository::class).lagreTemaAvklaring(behandlingId, true, Tema.AAP)
        repositoryProvider.provide(SaksnummerRepository::class).lagreSakVurdering(behandlingId, Saksvurdering("23452345"))
        repositoryProvider.provide(KategoriVurderingRepository::class).lagreKategoriseringVurdering(behandlingId, InnsendingType.SØKNAD)
        repositoryProvider.provide(StruktureringsvurderingRepository::class).lagreStrukturertDokument(
            behandlingId,
            """{"søknadsDato":"2024-09-02T22:00:00.000Z", "yrkesskade":"nei", "student": {"erStudent":"Nei"}}"""
        )
        return behandlingId
    }

    private fun hentAvklaringsbehov(behandlingId: BehandlingId, connection: DBConnection): Avklaringsbehovene {
        return AvklaringsbehovRepositoryImpl(connection).hentAvklaringsbehovene(behandlingId)
    }

}
