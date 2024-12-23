package no.nav.aap.postmottak.graphql

import no.nav.aap.lookup.gateway.GatewayProvider
import no.nav.aap.lookup.gateway.GatewayRegistry
import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tilJournalpost
import no.nav.aap.postmottak.gateway.Fagsystem
import no.nav.aap.postmottak.gateway.JournalpostGateway
import no.nav.aap.postmottak.gateway.SafSak
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.journalpostogbehandling.journalpost.Person
import no.nav.aap.postmottak.saf.graphql.SafGraphqlClientCredentialsClient
import no.nav.aap.postmottak.test.fakes.arenaSakerRespons
import no.nav.aap.postmottak.test.fakes.safFake
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.*


class SafGraphqlKlientTest: WithFakes {
    @BeforeEach
    fun setup() {
        GatewayRegistry.register(SafGraphqlClientCredentialsClient::class)
    }

    @Test
    fun hentJournalpost() {
        val person =  Person(123, identifikator = UUID.randomUUID(), identer = listOf(Ident("12345678")))
        val test = GatewayProvider.provide(JournalpostGateway::class).hentJournalpost(JournalpostId(1)).tilJournalpost(person)

        assertThat(test.journalpostId).isEqualTo(JournalpostId(1))
    }
    
    @Test
    fun `Kan parse saker`() {
        WithFakes.fakes.saf.setCustomModule { 
            safFake(sakerRespons = arenaSakerRespons())
        }
        val saker = GatewayProvider.provide(JournalpostGateway::class).hentSaker("12345678910")
        assertThat(saker).isEqualTo(listOf(SafSak("AAP", Fagsystem.AO01.name)))
    }
}