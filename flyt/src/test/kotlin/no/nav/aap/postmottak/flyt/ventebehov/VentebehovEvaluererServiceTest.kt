package no.nav.aap.postmottak.flyt.ventebehov

import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Definisjon
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.kontrakt.steg.StegType
import no.nav.aap.postmottak.test.fakes.InMemoryAvklaringsbehovRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID

class VentebehovEvaluererServiceTest {
    @Test
    fun `utløpt frist avslutter vent selv uten spesifikk evaluator`() {
        val behandlingId = BehandlingId(UUID.randomUUID().leastSignificantBits)
        val kontekst = FlytKontekst(JournalpostId(behandlingId.id), behandlingId, TypeBehandling.Journalføring)
        val avklaringsbehovene = InMemoryAvklaringsbehovRepository.hentAvklaringsbehovene(behandlingId)
        avklaringsbehovene.leggTil(
            definisjon = Definisjon.MANUELT_SATT_PÅ_VENT,
            stegType = StegType.AVKLAR_TEMA,
            frist = LocalDate.now(),
            grunn = ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER,
        )

        val løste = VentebehovEvaluererServiceImpl(emptyList()).løsVentebehov(kontekst, avklaringsbehovene)

        assertThat(løste.map { it.definisjon }).containsExactly(Definisjon.MANUELT_SATT_PÅ_VENT)
        assertThat(avklaringsbehovene.erSattPåVent()).isFalse()
        assertThat(avklaringsbehovene.hvemSomLøste(Definisjon.MANUELT_SATT_PÅ_VENT)).isEqualTo(SYSTEMBRUKER)
    }
}
