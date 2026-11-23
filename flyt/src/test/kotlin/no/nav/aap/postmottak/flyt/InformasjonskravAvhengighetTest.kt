package no.nav.aap.postmottak.flyt

import io.mockk.mockk
import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskrav
import no.nav.aap.postmottak.faktagrunnlag.InformasjonskravGrunnlagImpl
import no.nav.aap.postmottak.faktagrunnlag.Informasjonskravkonstruktør
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostInformasjonskrav
import no.nav.aap.postmottak.forretningsflyt.steg.StartBehandlingSteg
import no.nav.aap.postmottak.forretningsflyt.steg.under18.AvklarUnder18Steg
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
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
        val utførteInformasjonskrav = mutableListOf<String>()
        val journalpost = TestInformasjonskrav("journalpost", utførteInformasjonskrav)
        val fødselsdato = TestInformasjonskrav("fødselsdato", utførteInformasjonskrav, setOf(journalpost))
        val informasjonskravliste = mutableListOf<Informasjonskravkonstruktør>(journalpost, fødselsdato)
        val flyt = BehandlingFlytBuilder()
            .medSteg(StartBehandlingSteg, informasjonskrav = informasjonskravliste)
            .medSteg(AvklarUnder18Steg)
            .build()
        informasjonskravliste.clear()

        val grunnlag = InformasjonskravGrunnlagImpl(mockk(), mockk())
        val kontekst = mockk<FlytKontekst>()
        flyt.forberedFlyt(StegType.START_BEHANDLING)
        grunnlag.oppdaterFaktagrunnlagForKravliste(flyt.faktagrunnlagForGjeldendeSteg(), kontekst)
        assertThat(utførteInformasjonskrav).containsExactly("journalpost", "fødselsdato")

        utførteInformasjonskrav.clear()
        flyt.forberedFlyt(StegType.AVKLAR_UNDER18)
        grunnlag.oppdaterFaktagrunnlagForKravliste(flyt.alleFaktagrunnlagFørGjeldendeSteg(), kontekst)
        assertThat(utførteInformasjonskrav).containsExactly("journalpost", "fødselsdato")
    }

    private class TestInformasjonskrav(
        private val navn: String,
        private val utførteInformasjonskrav: MutableList<String>,
        override val avhengigheter: Set<Informasjonskravkonstruktør> = emptySet(),
    ) : Informasjonskravkonstruktør {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider) =
            object : Informasjonskrav {
                override fun oppdater(kontekst: FlytKontekst): Informasjonskrav.Endret {
                    utførteInformasjonskrav.add(navn)
                    return Informasjonskrav.Endret.IKKE_ENDRET
                }
            }
    }
}
