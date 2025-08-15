package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.behandlingsflyt.kontrakt.sak.Saksnummer
import no.nav.aap.behandlingsflyt.test.MockConnection
import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.komponenter.repository.RepositoryRegistry
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.gateway.Klagebehandling
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.klient.createGatewayProvider
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.DigitaliseringsvurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.OverleveringVurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.full.companionObjectInstance

class AvklaringsbehovsLøserTest {
    val repositoryRegistry = RepositoryRegistry().register<SaksnummerRepositoryImpl>()
        .register<AvklarTemaRepositoryImpl>()
        .register<DigitaliseringsvurderingRepositoryImpl>()
        .register<JournalpostRepositoryImpl>()
        .register<OverleveringVurderingRepositoryImpl>()

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        val løsningSubtypes = utledSubtypes.map {
            (it.companionObjectInstance as LøserKonstruktør<*>)
                .konstruer(
                    repositoryRegistry.provider(MockConnection().toDBConnection()),
                    createGatewayProvider {
                        register<BehandlingsflytGatewayMock>()
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

    override fun finnSaker(ident: Ident): List<BehandlingsflytSak> {
        TODO("Not yet implemented")
    }

    override fun sendHendelse(
        journalpostId: JournalpostId,
        kanal: KanalFraKodeverk,
        mottattDato: LocalDateTime,
        innsendingstype: InnsendingType,
        saksnummer: String,
        melding: Melding?
    ) {
        TODO("Not yet implemented")
    }

    override fun finnKlagebehandlinger(saksnummer: Saksnummer): List<Klagebehandling> {
        TODO("Not yet implemented")
    }
}