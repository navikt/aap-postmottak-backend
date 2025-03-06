package no.nav.aap.postmottak.avklaringsbehov.løser

import io.mockk.mockk
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.dokumenter.KanalFraKodeverk
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.DigitaliseringsvurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.OverleveringVurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.journalpost.JournalpostRepositoryImpl
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.reflect.full.primaryConstructor

class AvklaringsbehovsLøserTest {
    @BeforeEach
    fun setup() {
        RepositoryRegistry.register<SaksnummerRepositoryImpl>()
            .register<AvklarTemaRepositoryImpl>()
            .register<DigitaliseringsvurderingRepositoryImpl>()
            .register<JournalpostRepositoryImpl>()
            .register<OverleveringVurderingRepositoryImpl>()
        GatewayRegistry.register<BehandlingsflytGatewayMock>()
    }

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        val løsningSubtypes = utledSubtypes.map { it.primaryConstructor!!.call(mockk<DBConnection>()).forBehov() }.toSet()

        Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
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
        mottattDato: LocalDate,
        innsendingstype: InnsendingType,
        saksnummer: String,
        melding: Melding?
    ) {
        TODO("Not yet implemented")
    }

}