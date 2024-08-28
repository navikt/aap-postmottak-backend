package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.avløp.DokumentflytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.BehandlingsFlytStoppetHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StoppetHendelseJobbUtfører::class.java)

class StoppetHendelseJobbUtfører private constructor() : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val payload = input.payload()

        val hendelse = DefaultJsonMapper.fromJson<DokumentflytStoppetHendelse>(payload)

        val hendelseTilOppgaveStyring = BehandlingsFlytStoppetHendelseDTO(
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingType = hendelse.behandlingType,
            opprettetTidspunkt = hendelse.opprettetTidspunkt,
            referanse = hendelse.referanse,
            status = hendelse.status,
        )

        log.info("Varsler hendelse til OppgaveStyring. Saksnummer: ${hendelseTilOppgaveStyring.referanse}")
        OppgavestyringGateway.varsleHendelse(hendelseTilOppgaveStyring)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return StoppetHendelseJobbUtfører()
        }

        override fun type(): String {
            return "flyt.hendelse"
        }

        override fun navn(): String {
            return "Oppgavestyrings hendelse"
        }

        override fun beskrivelse(): String {
            return "Produsere hendelse til oppgavestyring"
        }
    }
}
