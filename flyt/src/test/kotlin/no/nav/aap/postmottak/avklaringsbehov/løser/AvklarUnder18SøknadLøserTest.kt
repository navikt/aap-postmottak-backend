package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.komponenter.httpklient.exception.UgyldigForespørselException
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.SYSTEMBRUKER
import no.nav.aap.postmottak.avklaringsbehov.AvklaringsbehovKontekst
import no.nav.aap.postmottak.avklaringsbehov.løsning.AvklarUnder18SøknadLøsning
import no.nav.aap.postmottak.journalpostogbehandling.behandling.BehandlingId
import no.nav.aap.postmottak.journalpostogbehandling.flyt.FlytKontekst
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.kontrakt.behandling.TypeBehandling
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.postmottak.test.fakes.InMemoryUnder18Repository
import no.nav.aap.postmottak.under18.Under18Søknadssituasjon
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AvklarUnder18SøknadLøserTest {
    private val løser = AvklarUnder18SøknadLøser.konstruer(
        RepositoryRegistry()
            .register<InMemoryUnder18Repository>()
            .provider(MockConnection().toDBConnection()),
        createGatewayProvider {}
    )

    @Test
    fun `for ung søker som vil fortsette avvises som ugyldig forespørsel uten lagring`() {
        val behandlingId = BehandlingId(UUID.randomUUID().leastSignificantBits)
        val kontekst = AvklaringsbehovKontekst(
            SYSTEMBRUKER,
            FlytKontekst(JournalpostId(behandlingId.id), behandlingId, TypeBehandling.Under18)
        )

        assertThrows<UgyldigForespørselException> {
            løser.løs(
                kontekst,
                AvklarUnder18SøknadLøsning(
                    situasjon = Under18Søknadssituasjon.SØKER_FOR_UNG,
                    søkerVilFortsette = true
                )
            )
        }
        assertThat(InMemoryUnder18Repository.hentSøknadsavklaring(behandlingId)).isNull()
    }
}
