package no.nav.aap.postmottak.flyt

import io.mockk.every
import io.mockk.mockk
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.IngenInput
import no.nav.aap.postmottak.faktagrunnlag.IngenRegisterData
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravNavn
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravRepository
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.AvklarUnder18Steg
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.under18.Under18FødselsdatoInformasjonskrav
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class InformasjonskravAvhengighetTest {
    @Test
    fun `fødselsdatoinformasjonskravet krever journalpostinformasjonskravet først i samme steg`() {
        val ugyldigeInformasjonskravlister = listOf(
            listOf(Under18FødselsdatoInformasjonskrav),
            listOf(Under18FødselsdatoInformasjonskrav, JournalpostInformasjonskrav)
        )

        ugyldigeInformasjonskravlister.forEach { informasjonskravliste ->
            assertThatThrownBy {
                BehandlingFlytBuilder().medSteg(AvklarUnder18Steg, informasjonskrav = informasjonskravliste)
            }.isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("kommer først i samme liste over informasjonskrav")
        }
    }

    @Test
    fun `informasjonskrav oppdateres i rekkefølge både i steget og ved gjenopptakelse`() {
        val utførteInformasjonskrav = mutableListOf<InformasjonskravNavn>()
        val journalpost = TestInformasjonskrav(InformasjonskravNavn.JOURNALPOST, utførteInformasjonskrav)
        val fødselsdato = TestInformasjonskrav(
            InformasjonskravNavn.UNDER18_FOEDSELSDATO, utførteInformasjonskrav, setOf(journalpost)
        )
        val informasjonskravliste = mutableListOf<Informasjonskravkonstruktør>(journalpost, fødselsdato)
        val flyt = BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg, informasjonskrav = informasjonskravliste)
            .medSteg(AvklarUnder18Steg)
            .build()
        informasjonskravliste.clear()

        val repositoryProvider = mockk<RepositoryProvider> {
            every { provide<InformasjonskravRepository>() } returns mockk(relaxed = true)
        }
        val grunnlag = InformasjonskravGrunnlagImpl(repositoryProvider, mockk())
        val kontekst = FlytKontekst(JournalpostId(1), BehandlingId(1), TypeBehandling.Under18)
        flyt.forberedFlyt(StegType.START_BEHANDLING)
        grunnlag.oppdaterFaktagrunnlagForKravliste(flyt.faktagrunnlagForGjeldendeSteg(), kontekst)
        assertThat(utførteInformasjonskrav).containsExactly(
            InformasjonskravNavn.JOURNALPOST, InformasjonskravNavn.UNDER18_FOEDSELSDATO
        )

        utførteInformasjonskrav.clear()
        flyt.forberedFlyt(StegType.AVKLAR_UNDER18)
        grunnlag.oppdaterFaktagrunnlagForKravliste(flyt.alleFaktagrunnlagFørGjeldendeSteg(), kontekst)
        assertThat(utførteInformasjonskrav).containsExactly(
            InformasjonskravNavn.JOURNALPOST, InformasjonskravNavn.UNDER18_FOEDSELSDATO
        )
    }

    private class TestInformasjonskrav(
        override val navn: InformasjonskravNavn,
        private val utførteInformasjonskrav: MutableList<InformasjonskravNavn>,
        override val avhengigheter: Set<Informasjonskravkonstruktør> = emptySet(),
    ) : Informasjonskravkonstruktør {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            object : Informasjonskrav<IngenInput, IngenRegisterData> {
                override fun klargjør(kontekst: FlytKontekst): IngenInput {
                    assertThat(utførteInformasjonskrav).containsAll(avhengigheter.map { it.navn })
                    return IngenInput
                }

                override fun hentData(input: IngenInput) = IngenRegisterData

                override fun oppdater(
                    input: IngenInput,
                    registerdata: IngenRegisterData,
                    kontekst: FlytKontekst,
                ): Informasjonskrav.Endret {
                    utførteInformasjonskrav.add(navn)
                    return Informasjonskrav.Endret.IKKE_ENDRET
                }
            }
    }
}
