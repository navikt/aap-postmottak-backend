package no.nav.aap.postmottak.server.prosessering

import no.nav.aap.komponenter.dbconnect.DBConnection
import no.nav.aap.postmottak.hendelse.avløp.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.hendelse.oppgave.BehandlingsFlytStoppetHendelseDTO
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StoppetHendelseJobbUtfører::class.java)

class StoppetHendelseJobbUtfører private constructor() : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val payload = input.payload()

        val hendelse = DefaultJsonMapper.fromJson<DokumentflytStoppetHendelse>(payload)

        val hendelseTilOppgave = BehandlingsFlytStoppetHendelseDTO(
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingType = hendelse.behandlingType,
            opprettetTidspunkt = hendelse.opprettetTidspunkt,
            referanse = hendelse.referanse,
            status = hendelse.status,
        )

        log.info("Varsler hendelse til Oppgave. Journalpost: ${hendelseTilOppgave.referanse}")
        // TODO: Uncomment når oppgave er klar
        //  OppgaveGateway.varsleHendelse(hendelseTilOppgave)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return StoppetHendelseJobbUtfører()
        }

        override fun type(): String {
            return "flyt.hendelse"
        }

        override fun navn(): String {
            return "Oppgavehendelse"
        }

        override fun beskrivelse(): String {
            return "Produsere hendelse til oppgave"
        }
    }
}
