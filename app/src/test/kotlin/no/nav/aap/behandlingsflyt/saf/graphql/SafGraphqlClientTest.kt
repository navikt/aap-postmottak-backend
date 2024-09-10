package no.nav.aap.behandlingsflyt.saf.graphql

import mottak.saf.SafGraphqlClient
import no.nav.aap.WithFakes
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test


class SafGraphqlClientTest: WithFakes() {

    @Test
    fun hentJournalpost() {
        val test = SafGraphqlClient.hentJournalpost(JournalpostId(1))

        assertThat(test.journalpostId).isEqualTo(JournalpostId(1))
    }
}