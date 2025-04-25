package no.nav.aap

import no.nav.aap.komponenter.gateway.Factory
import no.nav.aap.postmottak.gateway.StatistikkGateway
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import org.slf4j.LoggerFactory

class FakeStatistikkKlient : StatistikkGateway {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val hendelser = mutableListOf<DokumentflytStoppetHendelse>()
    companion object : Factory<FakeStatistikkKlient> {
        override fun konstruer(): FakeStatistikkKlient {
            return FakeStatistikkKlient()
        }
    }

    override fun avgiHendelse(oppgaveHendelse: DokumentflytStoppetHendelse) {
        logger.info("Mottok statistikkhendelse.")
        hendelser.add(oppgaveHendelse)
    }
}