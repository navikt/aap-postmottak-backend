package no.nav.aap.postmottak.hendelse.avløp

import no.nav.aap.komponenter.json.DefaultJsonMapper
import no.nav.aap.komponenter.miljo.Miljø
import no.nav.aap.motor.FlytJobbRepository
import no.nav.aap.motor.JobbInput
import no.nav.aap.postmottak.avklaringsbehov.Avklaringsbehovene
import no.nav.aap.postmottak.avklaringsbehov.løser.ÅrsakTilSettPåVent
import no.nav.aap.postmottak.faktagrunnlag.saksbehandler.dokument.JournalpostRepository
import no.nav.aap.postmottak.gateway.BehandlingsflytGateway
import no.nav.aap.postmottak.journalpostogbehandling.Ident
import no.nav.aap.postmottak.journalpostogbehandling.behandling.Behandling
import no.nav.aap.postmottak.kontrakt.hendelse.AvklaringsbehovHendelseDto
import no.nav.aap.postmottak.kontrakt.hendelse.DokumentflytStoppetHendelse
import no.nav.aap.postmottak.kontrakt.hendelse.EndringDTO
import no.nav.aap.postmottak.prosessering.StoppetHendelseJobbUtfører
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

private val log = LoggerFactory.getLogger(BehandlingHendelseServiceImpl::class.java)

class BehandlingHendelseServiceImpl(
    private val flytJobbRepository: FlytJobbRepository,
    private val journalpostRepository: JournalpostRepository,
    private val behandlingFlytGateway: BehandlingsflytGateway
) : BehandlingHendelseService {

    override fun stoppet(behandling: Behandling, avklaringsbehovene: Avklaringsbehovene) {

        val ident = journalpostRepository.hentHvisEksisterer(behandling.id)!!.person.aktivIdent().identifikator

        // TODO kun i dev inntil den er verifisert
        val nyesteSakForBruker = if (!Miljø.erProd()) {
            behandlingFlytGateway.finnSaker(Ident(ident)).maxByOrNull { it.periode.tom }
        } else null

        val hendelse = DokumentflytStoppetHendelse(
            journalpostId = behandling.journalpostId,
            saksnummer = nyesteSakForBruker?.saksnummer,
            ident = ident,
            referanse = behandling.referanse.referanse, // TODO må håndtere referanseendring i oppgave
            behandlingType = behandling.typeBehandling,
            status = behandling.status(),
            avklaringsbehov = avklaringsbehovene.alle().map { avklaringsbehov ->
                AvklaringsbehovHendelseDto(
                    avklaringsbehovDefinisjon = avklaringsbehov.definisjon,
                    status = avklaringsbehov.status(),
                    endringer = avklaringsbehov.historikk.map { endring ->
                        EndringDTO(
                            status = endring.status,
                            tidsstempel = endring.tidsstempel,
                            endretAv = endring.endretAv,
                            frist = endring.frist,
                            årsakTilSattPåVent = when (endring.grunn) {
                                ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER -> no.nav.aap.postmottak.kontrakt.hendelse.ÅrsakTilSettPåVent.VENTER_PÅ_OPPLYSNINGER
                                null -> null
                                else -> TODO("Skal ikke kunne skje")
                            }
                        )
                    })
            },
            opprettetTidspunkt = behandling.opprettetTidspunkt,
            hendelsesTidspunkt = LocalDateTime.now(),
        )

        val payload = DefaultJsonMapper.toJson(hendelse)

        log.info("Legger til flytjobber og stoppethendelse for oppgave for behandling: ${behandling.id}")
        flytJobbRepository.leggTil(
            JobbInput(jobb = StoppetHendelseJobbUtfører).medPayload(payload)
        )

    }
}
