package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.BehandlingsFlytStoppetHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.oppgavestyring.OppgavestyringGateway
import no.nav.aap.behandlingsflyt.sakogbehandling.sak.SakService
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører

class StoppetHendelseJobbUtfører private constructor(private val sakService: SakService) : JobbUtfører {

    override fun utfør(input: JobbInput) {
        val payload = input.payload()

        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(payload)

        val sak = sakService.hent(hendelse.sakID)

        val hendelseTilOppgaveStyring = BehandlingsFlytStoppetHendelseDTO(
            avklaringsbehov = hendelse.avklaringsbehov,
            behandlingType = hendelse.behandlingType,
            opprettetTidspunkt = hendelse.opprettetTidspunkt,
            referanse = hendelse.referanse,
            personident = sak.person.aktivIdent().identifikator,
            saksnummer = sak.saksnummer,
            status = hendelse.status,
            )

        OppgavestyringGateway.varsleHendelse(hendelseTilOppgaveStyring)
    }

    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            val sakService = SakService(connection)
            return StoppetHendelseJobbUtfører(sakService)
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
