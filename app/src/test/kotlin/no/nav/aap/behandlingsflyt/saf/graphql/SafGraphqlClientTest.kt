package no.nav.aap.behandlingsflyt.saf.graphql

import mottak.saf.SafGraphqlClient
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.dokumenter.JournalpostId
import no.nav.aap.behandlingsflyt.test.Fakes
import org.assertj.core.api.Assertions.*
import org.junit.jupiter.api.Test


class SafGraphqlClientTest {

    val fakes = Fakes(azurePort = 8081)

    @Test
    fun hentJournalpost() {
        val test = SafGraphqlClient.hentJournalpost(JournalpostId(1))

        assertThat(test.journalpostId).isEqualTo(1L)
    }
}