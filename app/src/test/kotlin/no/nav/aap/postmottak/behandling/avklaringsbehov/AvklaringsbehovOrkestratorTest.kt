package no.nav.aap.postmottak.behandling.avklaringsbehov

import no.nav.aap.behandlingsflyt.hendelse.mottak.BehandlingSattPåVent
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.komponenter.httpklient.auth.Bruker
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.postmottak.behandling.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.hendelse.avløp.BehandlingHendelseService
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.behandling.BehandlingRepositoryImpl
import no.nav.aap.postmottak.server.prosessering.StoppetHendelseJobbUtfører
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.junit.jupiter.api.Test
import java.time.LocalDate

class AvklaringsbehovOrkestratorTest {
    
    @Test
    fun `behandlingHendelseService dot stoppet blir kalt når en behandling er satt på vent`() {
        val uthentedeJobber = InitTestDatabase.dataSource.transaction { connection ->
            val behandlingHendelseService =
                BehandlingHendelseService(
                    FlytJobbRepository(connection),
                )

            val avklaringsbehovOrkestrator = AvklaringsbehovOrkestrator(
                connection,
                behandlingHendelseService,
            )

            val behandlingRepository = BehandlingRepositoryImpl(connection)
            val behandlingId = behandlingRepository.opprettBehandling(JournalpostId(11111))
            val behandling = behandlingRepository.hentMedLås(behandlingId, null)

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
                { it == StoppetHendelseJobbUtfører.type() },
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