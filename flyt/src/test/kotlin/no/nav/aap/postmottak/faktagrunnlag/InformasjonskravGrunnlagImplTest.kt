package no.nav.aap.postmottak.faktagrunnlag

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.repository.postgresRepositoryRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class InformasjonskravGrunnlagImplTest {
    private lateinit var dataSource: TestDataSource

    @BeforeEach
    fun setup() {
        dataSource = TestDataSource()
    }

    @AfterEach
    fun tearDown() = dataSource.close()

    @Test
    fun `registrerer kjøring også uten endringer men returnerer bare endrede informasjonskrav`() {
        dataSource.transaction { connection ->
            val provider = postgresRepositoryRegistry.provider(connection)
            val journalpostId = JournalpostId(1)
            val behandlingId = provider.provide<BehandlingRepository>()
                .opprettBehandling(journalpostId, TypeBehandling.Journalføring)
            val kontekst = FlytKontekst(journalpostId, behandlingId, TypeBehandling.Journalføring)
            val grunnlag = InformasjonskravGrunnlagImpl(provider, createGatewayProvider {})
            val endretInput = TestInput("journalpost")
            val uendretInput = TestInput("saksnummer")
            val endret = testinformasjonskrav(InformasjonskravNavn.JOURNALPOST, endretInput) {
                Informasjonskrav.Endret.ENDRET
            }
            val uendret = testinformasjonskrav(InformasjonskravNavn.SAKSNUMMER, uendretInput) {
                Informasjonskrav.Endret.IKKE_ENDRET
            }
            val før = Instant.now().truncatedTo(ChronoUnit.MILLIS)

            val resultat = grunnlag.oppdaterFaktagrunnlagForKravliste(
                listOf(StegType.AVKLAR_TEMA to endret, StegType.AVKLAR_TEMA to uendret),
                kontekst,
            )

            assertThat(resultat).containsExactly(endret)
            val oppdateringer = provider.provide<InformasjonskravRepository>()
                .hentOppdateringer(journalpostId, InformasjonskravNavn.entries.toList())
            assertThat(oppdateringer.map { it.navn }).containsExactlyInAnyOrder(
                InformasjonskravNavn.JOURNALPOST, InformasjonskravNavn.SAKSNUMMER
            )
            assertThat(oppdateringer).allSatisfy {
                assertThat(it.behandlingId).isEqualTo(behandlingId)
                assertThat(it.oppdatert).isBetween(før, Instant.now())
            }
            assertThat(oppdateringer.map { it.oppdatert }.distinct()).hasSize(1)
            assertThat(oppdateringer.associate { it.navn to it.forrigeInput<TestInput>() }).containsExactlyInAnyOrderEntriesOf(
                mapOf(InformasjonskravNavn.JOURNALPOST to endretInput, InformasjonskravNavn.SAKSNUMMER to uendretInput)
            )
            assertThat(oppdateringer).allSatisfy {
                val lagretInput = DefaultJsonMapper.fromJson<Map<String, String>>(requireNotNull(it.forrigeInput))
                assertThat(lagretInput).containsOnlyKeys("oppslag")
            }
        }
    }

    private fun testinformasjonskrav(
        navn: InformasjonskravNavn,
        input: TestInput,
        oppdater: () -> Informasjonskrav.Endret,
    ): Informasjonskravkonstruktør = object : Informasjonskravkonstruktør {
        override val navn = navn

        override fun konstruer(
            repositoryProvider: RepositoryProvider,
            gatewayProvider: GatewayProvider,
        ): Informasjonskrav<*, *> = object : Informasjonskrav<TestInput, TestRegisterdata> {
            override fun klargjør(kontekst: FlytKontekst) = input

            override fun hentData(input: TestInput) = TestRegisterdata("Skal ikke lagres i kjøringshistorikken")

            override fun oppdater(
                input: TestInput,
                registerdata: TestRegisterdata,
                kontekst: FlytKontekst,
            ) = oppdater()
        }
    }

    data class TestInput(val oppslag: String) : InformasjonskravInput

    data class TestRegisterdata(val opplysning: String) : InformasjonskravRegisterdata
}
