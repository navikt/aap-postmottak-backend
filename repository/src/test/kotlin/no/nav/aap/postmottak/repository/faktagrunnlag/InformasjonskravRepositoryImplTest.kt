package no.nav.aap.postmottak.repository.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.postmottak.faktagrunnlag.IngenInput
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravOppdatert
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class InformasjonskravRepositoryImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `henter siste kjøring per informasjonskrav på tvers av behandlinger for samme journalpost`() {
        val journalpostId = JournalpostId(1)
        val førsteTidspunkt = Instant.parse("2026-11-23T10:00:00.123Z")
        val sisteTidspunkt = førsteTidspunkt.plusSeconds(60)
        val informasjonskrav = listOf(InformasjonskravNavn.JOURNALPOST, InformasjonskravNavn.SAKSNUMMER)
        val input = informasjonskrav.associateWith { IngenInput }

        val forventedeOppdateringer = dataSource.transaction { connection ->
            val provider = postgresRepositoryRegistry.provider(connection)
            val behandlinger = provider.provide<BehandlingRepository>()
            val repository = provider.provide<InformasjonskravRepository>()
            val første = behandlinger.opprettBehandling(journalpostId, TypeBehandling.Fordeling)
            val siste = behandlinger.opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val annen = behandlinger.opprettBehandling(JournalpostId(2), TypeBehandling.Journalføring)

            assertThat(repository.hentOppdateringer(journalpostId, informasjonskrav)).isEmpty()
            repository.registrerOppdateringer(journalpostId, første, input, førsteTidspunkt)
            repository.registrerOppdateringer(journalpostId, første, input, sisteTidspunkt)
            repository.registrerOppdateringer(
                journalpostId, siste, mapOf(InformasjonskravNavn.JOURNALPOST to IngenInput), sisteTidspunkt
            )
            repository.registrerOppdateringer(JournalpostId(2), annen, input, sisteTidspunkt.plusSeconds(60))

            listOf(
                InformasjonskravOppdatert(siste, InformasjonskravNavn.JOURNALPOST, sisteTidspunkt, "{}"),
                InformasjonskravOppdatert(første, InformasjonskravNavn.SAKSNUMMER, sisteTidspunkt, "{}"),
            )
        }

        dataSource.transaction { connection ->
            val repository = postgresRepositoryRegistry.provider(connection).provide<InformasjonskravRepository>()
            assertThat(repository.hentOppdateringer(journalpostId, informasjonskrav))
                .containsExactlyInAnyOrderElementsOf(forventedeOppdateringer)
            assertThat(repository.hentOppdateringer(journalpostId, listOf(InformasjonskravNavn.SAKSNUMMER)))
                .containsExactly(forventedeOppdateringer.last())
            assertThat(repository.hentOppdateringer(journalpostId, emptyList())).isEmpty()

            val antall = connection.queryFirst<Long>("SELECT COUNT(*) AS ANTALL FROM INFORMASJONSKRAV_OPPDATERT") {
                setRowMapper { it.getLong("ANTALL") }
            }
            assertThat(antall).isEqualTo(7)
        }
    }

    @Test
    fun `henter lagret input og støtter historikk uten input`() {
        val journalpostId = JournalpostId(1)
        val input = JournalpostInformasjonskrav.Input(journalpostId)
        val tidspunkt = Instant.parse("2026-11-23T10:00:00Z")
        val behandlingId = dataSource.transaction { connection ->
            val provider = postgresRepositoryRegistry.provider(connection)
            val behandlingId = provider.provide<BehandlingRepository>()
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            connection.execute(
                """
                INSERT INTO INFORMASJONSKRAV_OPPDATERT (JOURNALPOST_ID, BEHANDLING_ID, INFORMASJONSKRAV, OPPDATERT)
                VALUES (?, ?, ?, ?)
                """.trimIndent()
            ) {
                setParams {
                    setLong(1, journalpostId.referanse)
                    setLong(2, behandlingId.id)
                    setEnumName(3, InformasjonskravNavn.JOURNALPOST)
                    setInstant(4, tidspunkt)
                }
            }
            behandlingId
        }

        dataSource.transaction { connection ->
            val repository = postgresRepositoryRegistry.provider(connection).provide<InformasjonskravRepository>()
            val tidligere = repository.hentOppdateringer(journalpostId, listOf(InformasjonskravNavn.JOURNALPOST)).single()
            assertThat(tidligere.forrigeInput<JournalpostInformasjonskrav.Input>()).isNull()

            repository.registrerOppdateringer(
                journalpostId, behandlingId, mapOf(InformasjonskravNavn.JOURNALPOST to input), tidspunkt.plusSeconds(60)
            )
        }

        dataSource.transaction { connection ->
            val repository = postgresRepositoryRegistry.provider(connection).provide<InformasjonskravRepository>()
            val oppdatert = repository.hentOppdateringer(journalpostId, listOf(InformasjonskravNavn.JOURNALPOST)).single()
            assertThat(oppdatert.forrigeInput<JournalpostInformasjonskrav.Input>()).isEqualTo(input)
        }
    }
}
