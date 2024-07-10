package no.nav.aap.behandlingsflyt.server.prosessering

import no.nav.aap.behandlingsflyt.dbconnect.DBConnection
import no.nav.aap.behandlingsflyt.hendelse.avløp.BehandlingFlytStoppetHendelse
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkGateway
import no.nav.aap.behandlingsflyt.hendelse.statistikk.StatistikkHendelseDTO
import no.nav.aap.behandlingsflyt.hendelse.statistikk.VilkårsResultatDTO
import no.nav.aap.json.DefaultJsonMapper
import no.nav.aap.motor.Jobb
import no.nav.aap.motor.JobbInput
import no.nav.aap.motor.JobbUtfører
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(StatistikkJobbUtfører::class.java)

enum class StatistikkType {
    BehandlingStoppet, VilkårsResultat
}

class StatistikkJobbUtfører(private val statistikkGateway: StatistikkGateway) : JobbUtfører {
    override fun utfør(input: JobbInput) {
        log.info("Utfører jobbinput statistikk: $input")
        val payload = input.payload()

        val type = StatistikkType.valueOf(input.parameter("statistikk-type"))

        when (type) {
            StatistikkType.BehandlingStoppet -> håndterBehandlingStoppet(payload)
            StatistikkType.VilkårsResultat -> håndterVilkårsResultat(payload)
        }

    }

    private fun håndterBehandlingStoppet(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<BehandlingFlytStoppetHendelse>(payload)

        statistikkGateway.avgiStatistikk(
            StatistikkHendelseDTO(
                saksnummer = hendelse.saksnummer.toString(),
                behandlingType = hendelse.behandlingType,
                status = hendelse.status
            )
        )
    }

    private fun håndterVilkårsResultat(payload: String) {
        val hendelse = DefaultJsonMapper.fromJson<VilkårsResultatDTO>(payload)
        statistikkGateway.vilkårsResultat(hendelse)
    }


    companion object : Jobb {
        override fun konstruer(connection: DBConnection): JobbUtfører {
            return StatistikkJobbUtfører(StatistikkGateway())
        }

        override fun type(): String {
            return "flyt.statistikk"
        }

        override fun navn(): String {
            return "Lagrer statistikk"
        }

        override fun beskrivelse(): String {
            return "Skal ta i mot data fra steg i en behandling og sender til statistikk-appen."
        }
    }
}