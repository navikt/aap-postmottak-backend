package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.postmottak.test.FakeUnleash
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.postmottak.test.MockConnection
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.BehandlingsflytSaksInfoTilPostmottak
import no.nav.aap.postmottak.gateway.Klagebehandling
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.klient.SystemKlokke
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.WithDependencies
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.companionObjectInstance

class AvklaringsbehovsLøserTest {

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        val løsningSubtypes = utledSubtypes.map {
            (it.companionObjectInstance as LøserKonstruktør<*>)
                .konstruer(
                    WithDependencies.repositoryRegistry.provider(MockConnection().toDBConnection()),
                    createGatewayProvider {
                        register<BehandlingsflytGatewayMock>()
                        register<FakeUnleash>()
                        register<SystemKlokke>()
                    })
                .forBehov()
        }.toSet()

        Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
    }

    @Test
    fun `alle subtyper skal ha unik verdi 2`() {
        val forventedeLøsere = setOf(
            AvklarOverleveringLøser::class,
            AvklarSakLøser::class,
            AvklarTemaLøser::class,
            AvklarUnder18SamtykkeLøser::class,
            AvklarUnder18SøknadLøser::class,
            DigitaliserDokumentLøser::class,
            SattPåVentLøser::class,
        )

        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses.toSet()

        Assertions.assertThat(utledSubtypes).isEqualTo(forventedeLøsere)
    }
}

class BehandlingsflytGatewayMock : BehandlingsflytGateway {
    companion object : Factory<BehandlingsflytGatewayMock> {
        override fun konstruer(): BehandlingsflytGatewayMock {
            return BehandlingsflytGatewayMock()
        }
    }

    override fun finnEllerOpprettSak(ident: Ident, mottattDato: LocalDate): BehandlingsflytSak {
        TODO("Not yet implemented")
    }

    override fun finnSaker(ident: Ident): List<BehandlingsflytSaksInfoTilPostmottak> {
        TODO("Not yet implemented")
    }

    override fun sendHendelse(
        journalpostId: JournalpostId,
        kanal: KanalFraKodeverk,
        mottattDato: LocalDateTime,
        innsendingstype: InnsendingType,
        saksnummer: String,
        melding: Melding?,
        digitalisertIPostmottak: Boolean

    ) {
        TODO("Not yet implemented")
    }

    override fun finnKlagebehandlinger(saksnummer: Saksnummer): List<Klagebehandling> {
        TODO("Not yet implemented")
    }
}