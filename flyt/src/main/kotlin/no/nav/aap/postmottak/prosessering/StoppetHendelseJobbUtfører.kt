package no.nav.aap.postmottak.prosessering

import no.nav.aap.komponenter.gateway.GatewayProvider
import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.lookup.repository.RepositoryProvider
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import no.nav.aap.motor.ProvidersJobbSpesifikasjon
import no.nav.aap.postmottak.gateway.OppgaveGateway
import no.nav.aap.postmottak.gateway.StatistikkGateway
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StoppetHendelseJobbUtfører::class.java)

class StoppetHendelseJobbUtfører private constructor(private val gatewayProvider: GatewayProvider) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val payload = input.payload()

        val hendelse = DefaultJsonMapper.fromJson<DokumentflytStoppetHendelse>(payload)

        log.info("Varsler hendelse til Oppgave: $hendelse")
        gatewayProvider.provide(OppgaveGateway::class).varsleHendelse(hendelse)
        log.info("Avgir hendelse til statistikk.")
        gatewayProvider.provide(StatistikkGateway::class).avgiHendelse(hendelse)
    }

    companion object : ProvidersJobbSpesifikasjon {
        override fun konstruer(repositoryProvider: RepositoryProvider, gatewayProvider: GatewayProvider): JobbUtfører {
            return StoppetHendelseJobbUtfører(gatewayProvider)
        }

        override val type: String = "flyt.hendelse"

        override val navn: String = "Oppgavehendelse"

        override val beskrivelse: String = "Produsere hendelse til oppgave"
    }
}
