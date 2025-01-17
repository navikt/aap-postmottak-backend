package no.nav.aap.postmottak.avklaringsbehov.løser

import no.nav.aap.behandlingsflyt.kontrakt.hendelse.InnsendingType
import no.nav.aap.behandlingsflyt.kontrakt.hendelse.dokumenter.Melding
import no.nav.aap.komponenter.dbconnect.transaction
import no.nav.aap.komponenter.dbtest.InitTestDatabase
import no.nav.aap.lookup.gateway.Factory
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.lookup.repository.RepositoryRegistry
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.gateway.BehandlingsflytSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Journalpost
import no.nav.aap.postmottak.repository.faktagrunnlag.AvklarTemaRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.KategorivurderingRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.SaksnummerRepositoryImpl
import no.nav.aap.postmottak.repository.faktagrunnlag.StruktureringsvurderingRepositoryImpl
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
            .register<StruktureringsvurderingRepositoryImpl>()
            .register<KategorivurderingRepositoryImpl>()
            .register<JournalpostRepositoryImpl>()
        GatewayRegistry.register<BehandlingsflytGatewayMock>()
    }

    @Test
    fun `alle subtyper skal ha unik verdi`() {
        val utledSubtypes = AvklaringsbehovsLøser::class.sealedSubclasses
        InitTestDatabase.dataSource.transaction { dbConnection ->
            val løsningSubtypes = utledSubtypes.map { it.primaryConstructor!!.call(dbConnection).forBehov() }.toSet()

            Assertions.assertThat(løsningSubtypes).hasSize(utledSubtypes.size)
        }
    }
}

class BehandlingsflytGatewayMock: BehandlingsflytGateway {
    companion object: Factory<BehandlingsflytGatewayMock> {
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
        journalpost: Journalpost,
        innsendingstype: InnsendingType,
        saksnummer: String,
        melding: Melding?
    ) {
        TODO("Not yet implemented")
    }

}