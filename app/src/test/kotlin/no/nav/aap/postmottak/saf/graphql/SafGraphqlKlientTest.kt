package no.nav.aap.postmottak.saf.graphql

import no.nav.aap.postmottak.test.fakes.WithFakes
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.tilJournalpost
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import no.nav.aap.postmottak.sakogbehandling.journalpost.Person
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
}