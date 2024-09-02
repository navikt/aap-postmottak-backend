package no.nav.aap.behandlingsflyt.hendelse.avløp

import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.behandlingsflyt.behandling.avklaringsbehov.Status
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.Behandling
import no.nav.aap.behandlingsflyt.sakogbehandling.behandling.flate.BehandlingReferanse
import no.nav.aap.behandlingsflyt.server.prosessering.StoppetHendelseJobbUtfører
import no.nav.aap.komponenter.httpklient.json.DefaultJsonMapper
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger(BehandlingHendelseService::class.java)

class BehandlingHendelseService(
    private val flytJobbRepository: FlytJobbRepository
) {

    /**
     * Kjøres når en behandling er avsluttet. For statistikkformål.
     */
    fun avsluttet(behandling: Behandling) {
        val vilkårsResultatDTO =
            AvsluttetBehandlingHendelseDTO(behandling.id)

        val payload = DefaultJsonMapper.toJson(vilkårsResultatDTO)

    }

    fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {

        // TODO: Utvide med flere parametere for prioritering
        val hendelse = DokumentflytStoppetHendelse(
            referanse = BehandlingReferanse(behandling.referanse.referanse),
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
            opprettetTidspunkt = behandling.opprettetTidspunkt
        )

        val payload = DefaultJsonMapper.toJson(hendelse)

        log.info("Legger til flytjobber til statistikk og stoppethendels for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
        )

    }
}
