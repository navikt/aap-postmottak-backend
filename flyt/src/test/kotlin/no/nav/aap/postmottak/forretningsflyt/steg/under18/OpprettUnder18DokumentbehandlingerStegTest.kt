package no.nav.aap.postmottak.forretningsflyt.steg.under18

import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.TestDataSource
import no.nav.aap.motor.FlytJobbRepositoryImpl
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.SaksnummerRepository
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.sak.Saksvurdering
import no.nav.aap.postmottak.gateway.DokarkivGateway
import no.nav.aap.postmottak.gateway.Journalstatus
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingRepository
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.DokumentInfoId
import no.nav.aap.postmottak.klient.defaultGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.test.Fakes
import no.nav.aap.postmottak.test.fakes.TestJournalposter
import no.nav.aap.postmottak.test.fakes.vedlegg
import no.nav.aap.postmottak.under18.Under18Repository
import no.nav.aap.postmottak.under18.Under18Søknadsgrunnlag
import no.nav.aap.postmottak.under18.Under18TestOppsett
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@Fakes
class OpprettUnder18DokumentbehandlingerStegTest {
    @ParameterizedTest
    @ValueSource(ints = [1, 2])
    fun `gjenbruker en tilknytning uten lagret svar og stopper ved flere treff`(antallTreff: Int) {
        val kilde = TestJournalposter.leggTil {
            status = Journalstatus.JOURNALFOERT
            dokumenter = listOf(vedlegg("Søknad").copy(dokumentInfoId = DokumentInfoId("489172$antallTreff")))
        }.tilJournalpost()
        val gateways = defaultGatewayProvider()
        val dokarkiv = gateways.provide<DokarkivGateway>()
        val sak = "S1-test"
        val tilknyttede = List(antallTreff) { dokarkiv.knyttTilAnnenSak(kilde, sak, "9999") }
        TestDataSource().use { dataSource ->
            val repositoryRegistry = Under18TestOppsett(dataSource, gateways).repositoryRegistry
                .register<FlytJobbRepositoryImpl>()
            dataSource.transaction { connection ->
                val repositories = repositoryRegistry.provider(connection)
                val behandlingId = repositories.provide<BehandlingRepository>()
                    .opprettBehandling(kilde.journalpostId, TypeBehandling.Under18)
                val under18 = repositories.provide<Under18Repository>()
                under18.lagreSøknadsgrunnlag(behandlingId, Under18Søknadsgrunnlag(kilde.person.id, listOf(kilde.journalpostId)))
                repositories.provide<SaksnummerRepository>().lagreSakVurdering(behandlingId, Saksvurdering(sak))
                val steg = OpprettUnder18DokumentbehandlingerSteg.konstruer(repositories, gateways)
                val kontekst = FlytKontekst(kilde.journalpostId, behandlingId, TypeBehandling.Under18)
                if (antallTreff == 1) {
                    val behandlinger = repositories.provide<BehandlingRepository>()
                    val eksisterende = behandlinger.opprettBehandling(tilknyttede.single(), TypeBehandling.DokumentHåndtering)
                    steg.utfør(kontekst)
                    assertThat(TestJournalposter.hentTilknyttedeJournalposter(kilde.hoveddokument.dokumentInfoId)
                        .filter { it.sak?.fagsakId == sak }.map { it.journalpostId })
                        .containsExactly(tilknyttede.single().referanse.toString())
                    assertThat(behandlinger.hentAlleBehandlingerForJournalpost(tilknyttede.single()).map { it.id })
                        .containsExactly(eksisterende)
                } else {
                    assertThatIllegalStateException().isThrownBy { steg.utfør(kontekst) }
                        .withMessageContaining("Flere tilknytninger")
                }
            }
        }
    }
}
