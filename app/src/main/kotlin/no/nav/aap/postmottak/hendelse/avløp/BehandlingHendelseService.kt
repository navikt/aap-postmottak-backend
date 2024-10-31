package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.kontrakt.avklaringsbehov.Status
import no.nav.aap.postmottak.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.postmottak.kontrakt.hendelse.DefinisjonDTO
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.kontrakt.hendelse.EndringDTO
import no.nav.aap.postmottak.sakogbehandling.behandling.Behandling
import no.nav.aap.postmottak.server.prosessering.StoppetHendelseJobbUtfører
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(BehandlingHendelseService::class.java)

class BehandlingHendelseService(
    private val flytJobbRepository: FlytJobbRepository
) {

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {

        // TODO: Utvide med flere parametere for prioritering
        val hendelse = DokumentflytStoppetHendelse(
            referanse = behandling.referanse.referanse, // TODO må håndtere referanseendring i oppgave
            behandlingType = behandling.typeBehandling,
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                AvklaringsbehovHendelseDto(definisjon = DefinisjonDTO(
                    type = avklaringsbehov.definisjon.kode,
                    behovType = avklaringsbehov.definisjon.type,
                    løsesISteg = avklaringsbehov.løsesISteg()
                ), status = avklaringsbehov.status(), endringer = avklaringsbehov.historikk.filter {
                    it.status in listOf(
                        Status.OPPRETTET, Status.SENDT_TILBAKE_FRA_BESLUTTER, Status.AVSLUTTET
                    )
                }.map { endring ->
                    EndringDTO(
                        status = endring.status,
                        tidsstempel = endring.tidsstempel,
                        endretAv = endring.endretAv,
                        frist = endring.frist
                    )
                })
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
        )

        val payload = DefaultJsonMapper.toJson(hendelse)

        log.info("Legger til flytjobber til statistikk og stoppethendels for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
        )

    }
}
