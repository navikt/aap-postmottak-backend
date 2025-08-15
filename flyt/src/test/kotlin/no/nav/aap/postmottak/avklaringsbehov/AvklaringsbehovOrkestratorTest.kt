package no.nav.aap.postmottak.avklaringsbehov

import io.mockk.mockk
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.postmottak.avklaringsbehov.løser.BehandlingsflytGatewayMock
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.flyt.FlytOrkestrator
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseServiceImpl
import no.nav.aap.postmottak.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.postmottak.repository.avklaringsbehov.AvklaringsbehovRepositoryImpl
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import no.nav.aap.postmottak.repository.person.PersonRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovOrkestratorTest {
    private val gatewayProvider = createGatewayProvider {
        register<BehandlingsflytGatewayMock>()
    }
    
    
    private val repositoryRegistry = RepositoryRegistry().register<PersonRepositoryImpl>()
        .register<JournalpostRepositoryImpl>()
        .register<PersonRepositoryImpl>()
        .register<BehandlingRepositoryImpl>()
        .register<AvklaringsbehovRepositoryImpl>()
        .register<FlytJobbRepositoryImpl>()

    @AfterEach
    fun afterEach() {
        InitTestDatabase.freshDatabase().transaction { it.execute("TRUNCATE behandling CASCADE ") }
    }


    @Test
    fun `behandlingHendelseService dot stoppet blir kalt når en behandling er satt på vent`() {
        val uthentedeJobber = InitTestDatabase.freshDatabase().transaction { connection ->
            val repositoryProvider = repositoryRegistry.provider(connection)
            val behandlingHendelseService =
                BehandlingHendelseServiceImpl(
                    repositoryProvider.provide(),
                    mockk(relaxed = true),
                    mockk(relaxed = true),
                )

            val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
                repositoryProvider,
                behandlingHendelseService,
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                repositoryProvider.provide(),
                FlytOrkestrator(repositoryProvider, gatewayProvider),
                gatewayProvider
            )

            val behandlingRepository = repositoryProvider.provide(BehandlingRepository::class)
            val behandlingId =
                behandlingRepository.opprettBehandling(JournalpostId(11111), TypeBehandling.Journalføring)
            val behandling = behandlingRepository.hent(behandlingId)

            // Act
            avklaringsbehovOrkestrator.settBehandlingPåVent(
                behandling.id,
                BehandlingSattPåVent(
                    frist = LocalDate.now().plusDays(1),
                    begrunnelse = "en god begrunnelse",
                    behandlingVersjon = behandling.versjon,
                    bruker = Bruker("123"),
                    grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
                )
            )

            hentJobber(connection)
        }
        assertThat(uthentedeJobber).haveAtLeastOne(
            Condition(
                { it == StoppetHendelseJobbUtfører.type },
                "skal være av rett type"
            )
        )
    }

    private fun hentJobber(connection: DBConnection): List<String> {
        return connection.queryList(
            """
            SELECT type FROM JOBB
        """.trimIndent()
        ) {
            setRowMapper {
                it.getString("type")
            }
        }
    }
}