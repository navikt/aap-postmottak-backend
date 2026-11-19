package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SamtykkeLøsning
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import no.nav.aap.postmottak.test.fakes.InMemoryUnder18Repository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate
import java.util.UUID

class AvklarUnder18SamtykkeLøserTest {
    private val løser = AvklarUnder18SamtykkeLøser.konstruer(
        RepositoryRegistry()
            .register<InMemoryUnder18Repository>()
            .register<InMemoryAvklaringsbehovRepository>()
            .provider(MockConnection().toDBConnection()),
        createGatewayProvider {}
    )

    @ParameterizedTest
    @ValueSource(ints = [7, 40])
    fun `manglende samtykke setter på vent i løseren med avkortet frist`(dagerTil18: Long) {
        val fødselsdato = LocalDate.now().minusYears(18).plusDays(dagerTil18)
        val kontekst = kontekst(fødselsdato)

        løser.løs(kontekst, AvklarUnder18SamtykkeLøsning(beggeForeldreSamtykket = false))

        val vent = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.kontekst.behandlingId)
            .hentVentepunkter().single()
        assertThat(vent.definisjon).isEqualTo(Definisjon.VENT_PA_UNDER18_SAMTYKKE)
        assertThat(vent.frist()).isEqualTo(minOf(LocalDate.now().plusWeeks(3), fødselsdato.plusYears(18)))
    }

    @Test
    fun `godkjent samtykke oppretter ikke vent`() {
        val kontekst = kontekst(LocalDate.now().minusYears(18).plusWeeks(1))

        løser.løs(kontekst, AvklarUnder18SamtykkeLøsning(beggeForeldreSamtykket = true))

        assertThat(
            InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.kontekst.behandlingId).hentVentepunkter()
        ).isEmpty()
    }

    @Test
    fun `godkjent samtykke tar behandlingen av vent`() {
        val kontekst = kontekst(LocalDate.now().minusYears(18).plusWeeks(1))
        løser.løs(kontekst, AvklarUnder18SamtykkeLøsning(beggeForeldreSamtykket = false))

        løser.løs(kontekst, AvklarUnder18SamtykkeLøsning(beggeForeldreSamtykket = true))

        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.kontekst.behandlingId)
        assertThat(avklaringsbehovene.erSattPåVent()).isFalse()
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VENT_PA_UNDER18_SAMTYKKE)!!.erAvsluttet())
            .isTrue()
    }

    private fun kontekst(fødselsdato: LocalDate): AvklaringsbehovKontekst {
        val behandlingId = BehandlingId(UUID.randomUUID().leastSignificantBits)
        InMemoryUnder18Repository.lagreFødselsdato(behandlingId, fødselsdato)
        return AvklaringsbehovKontekst(
            SYSTEMBRUKER,
            FlytKontekst(JournalpostId(behandlingId.id), behandlingId, TypeBehandling.Under18)
        )
    }
}
