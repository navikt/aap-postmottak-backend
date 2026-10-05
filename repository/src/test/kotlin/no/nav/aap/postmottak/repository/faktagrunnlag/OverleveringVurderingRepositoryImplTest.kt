package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.overlever.OverleveringVurdering
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.behandling.BehandlingRepositoryImpl
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OverleveringVurderingRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun beforeEach() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `lagre og hente`() {
        val overleveringVurdering = OverleveringVurdering(
            skalOverleveresTilKelvin = true,
            begrunnelse = "en begrunnelse"
        )
        val behandlingId = dataSource.transaction { connection ->
            val behandlingId = BehandlingRepositoryImpl(connection)
                .opprettBehandling(JournalpostId(1), TypeBehandling.Journalføring)

            OverleveringVurderingRepositoryImpl(connection).lagre(
                behandlingId = behandlingId,
                overleveringVurdering = overleveringVurdering
            )

            behandlingId
        }

        val uthentet = dataSource.transaction { connection ->
            OverleveringVurderingRepositoryImpl(connection).hentHvisEksisterer(behandlingId)
        }

        assertThat(uthentet).isEqualTo(overleveringVurdering)

    }

}