package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import no.nav.aap.postmottak.test.fakes.InMemoryUnder18Repository
import no.nav.aap.postmottak.under18.Under18SamtykkeVurdering
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class Under18SamtykkeVurderingVentebehovEvaluererTest {
    private val service = VentebehovEvaluererServiceImpl(
        listOf(Under18SamtykkeVurderingVentebehovEvaluerer(InMemoryUnder18Repository))
    )

    @Test
    fun `positiv aktiv samtykkevurdering avslutter bare samtykkevent`() {
        val kontekst = kontekst()
        val avklaringsbehovene = opprettSamtykkevent(kontekst)
        avklaringsbehovene.leggTil(
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            stegType = StegType.AVKLAR_UNDER18_SAMTYKKE,
            frist = LocalDate.now().plusDays(1),
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
        )
        lagreSamtykkevurdering(kontekst, beggeForeldreSamtykket = true)

        val løste = service.løsVentebehov(kontekst, avklaringsbehovene)

        assertThat(løste.map { it.definisjon }).containsExactly(Definisjon.VENT_PA_UNDER18_SAMTYKKE)
        val samtykkevent = avklaringsbehovene.hentBehovForDefinisjon(Definisjon.VENT_PA_UNDER18_SAMTYKKE)!!
        assertThat(samtykkevent.status()).isEqualTo(Status.AVSLUTTET)
        assertThat(samtykkevent.endretAv()).isEqualTo(SYSTEMBRUKER.ident)
        assertThat(avklaringsbehovene.hentBehovForDefinisjon(Definisjon.MANUELT_SATT_PÅ_VENT)!!.erÅpent()).isTrue()
    }

    @Test
    fun `manglende samtykkevurdering holder vent åpent selv om en annen behandling har samtykke`() {
        val kontekst = kontekst()
        val avklaringsbehovene = opprettSamtykkevent(kontekst)
        lagreSamtykkevurdering(kontekst(), beggeForeldreSamtykket = true)

        val løste = service.løsVentebehov(kontekst, avklaringsbehovene)

        assertThat(løste).isEmpty()
        assertThat(avklaringsbehovene.erSattPåVent()).isTrue()
    }

    @Test
    fun `siste negative samtykkevurdering holder vent åpent`() {
        val kontekst = kontekst()
        val avklaringsbehovene = opprettSamtykkevent(kontekst)
        lagreSamtykkevurdering(kontekst, beggeForeldreSamtykket = true)
        lagreSamtykkevurdering(kontekst, beggeForeldreSamtykket = false)

        val løste = service.løsVentebehov(kontekst, avklaringsbehovene)

        assertThat(løste).isEmpty()
        assertThat(avklaringsbehovene.erSattPåVent()).isTrue()
    }

    private fun kontekst(): FlytKontekst {
        val behandlingId = BehandlingId(UUID.randomUUID().leastSignificantBits)
        return FlytKontekst(JournalpostId(behandlingId.id), behandlingId, TypeBehandling.Under18)
    }

    private fun opprettSamtykkevent(kontekst: FlytKontekst): Avklaringsbehovene =
        InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(kontekst.behandlingId).also {
            it.leggTil(
                definisjon = Definisjon.VENT_PA_UNDER18_SAMTYKKE,
                stegType = StegType.AVKLAR_UNDER18_SAMTYKKE,
                frist = LocalDate.now().plusDays(1),
                grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
            )
        }

    private fun lagreSamtykkevurdering(kontekst: FlytKontekst, beggeForeldreSamtykket: Boolean) {
        InMemoryUnder18Repository.lagreSamtykkevurdering(
            kontekst.behandlingId,
            Under18SamtykkeVurdering(beggeForeldreSamtykket, emptyList()),
        )
    }
}
