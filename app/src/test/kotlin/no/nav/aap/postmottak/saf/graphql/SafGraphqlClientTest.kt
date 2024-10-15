package no.nav.aap.postmottak.saf.graphql

import no.nav.aap.WithFakes
import no.nav.aap.postmottak.kontrakt.journalpost.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class SafGraphqlClientTest: WithFakes {

    @Test
    fun hentJournalpost() {
        val test = SafGraphqlClient.withClientCredentialsRestClient().hentJournalpost(JournalpostId(1)).tilJournalpost()

        assertThat(test.journalpostId).isEqualTo(JournalpostId(1))
    }
}