package no.nav.aap.postmottak.gateway

import no.nav.aap.lookup.gateway.Gateway
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse

interface StatistikkGateway : Gateway{
    fun avgiHendelse(oppgaveHendelse: DokumentflytStoppetHendelse)
}