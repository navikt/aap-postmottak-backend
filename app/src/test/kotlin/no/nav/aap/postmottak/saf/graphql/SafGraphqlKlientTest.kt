package no.nav.aap.postmottak.saf.graphql

import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tilJournalpost
import no.nav.aap.postmottak.klient.joark.Fagsystem
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
import no.nav.aap.postmottak.test.fakes.arenaSakerRespons
import no.nav.aap.postmottak.test.fakes.safFake
import no.nav.aap.verdityper.sakogbehandling.Ident
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*


class SafGraphqlKlientTest: WithFakes {

    @Test
    fun hentJournalpost() {
        val person =  Person(123, identifikator = UUID.randomUUID(), identer = listOf(Ident("12345678")))
        val test = SafGraphqlKlient.withClientCredentialsRestClient().hentJournalpost(JournalpostId(1)).tilJournalpost(person)

        assertThat(test.journalpostId).isEqualTo(JournalpostId(1))
    }
    
    @Test
    fun `Kan parse saker`() {
        WithFakes.fakes.saf.setCustomModule { 
            safFake(sakerRespons = arenaSakerRespons())
        }
        val saker = SafGraphqlKlient.withClientCredentialsRestClient().hentSaker("12345678910")
        assertThat(saker).isEqualTo(listOf(SafSak("AAP", Fagsystem.AO01.name)))
    }
}