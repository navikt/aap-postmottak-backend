package no.nav.aap.behandlingsflyt.prosessering.retry

import no.nav.aap.behandlingsflyt.dbstuff.DBConnection
import no.nav.aap.behandlingsflyt.prosessering.Oppgave
import no.nav.aap.behandlingsflyt.prosessering.OppgaveInput
import org.slf4j.LoggerFactory

internal const val OPPGAVE_TYPE = "oppgave.retryFeilede"

class RekjørFeiledeOppgaver : Oppgave() {

    private val log = LoggerFactory.getLogger(RekjørFeiledeOppgaver::class.java)

    override fun utfør(connection: DBConnection, input: OppgaveInput) {
        val repository = RetryFeiledeOppgaverRepository(connection)

        val feilendeOppgaverMarkertForRekjøring = repository.markerAlleFeiledeForKlare()
        log.info("Markert {} oppgaver for rekjøring", feilendeOppgaverMarkertForRekjøring)

        repository.planleggNyKjøring()
    }

    override fun type(): String {
        return OPPGAVE_TYPE
    }
}